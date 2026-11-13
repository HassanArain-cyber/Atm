import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

// Java ATM web edition: same OOP concepts as the original console application.
abstract class WebBankAccount {
    private final int number; private final String owner, pin;
    private long balanceCents;
    private final String[] history = new String[50]; private int count;
    WebBankAccount(int number,String owner,String pin,long cents){this.number=number;this.owner=owner;this.pin=pin;this.balanceCents=cents;record("Account opened");}
    int number(){return number;} String owner(){return owner;} long balance(){return balanceCents;}
    boolean pinMatches(String value){return pin.equals(value);}
    void deposit(long cents){if(cents<=0)throw new IllegalArgumentException("Enter an amount greater than zero.");balanceCents=Math.addExact(balanceCents,cents);record("Deposited Rs. "+money(cents));}
    void withdraw(long cents){if(cents<=0)throw new IllegalArgumentException("Enter an amount greater than zero.");if(cents>balanceCents)throw new IllegalArgumentException("Insufficient balance.");balanceCents-=cents;record("Withdrew Rs. "+money(cents));}
    void record(String entry){if(count==history.length){for(int i=1;i<count;i++)history[i-1]=history[i];count--;}history[count++]=entry;}
    String[] history(){return Arrays.copyOf(history,count);}
    abstract String type(); // abstraction + runtime polymorphism
    static String money(long cents){return String.format(Locale.ROOT,"%.2f",cents/100.0);}
}
class WebSavingsAccount extends WebBankAccount {WebSavingsAccount(int n,String o,String p,long c){super(n,o,p,c);} @Override String type(){return "Savings";}}
class WebCurrentAccount extends WebBankAccount {WebCurrentAccount(int n,String o,String p,long c){super(n,o,p,c);} @Override String type(){return "Current";}}
public class ATMWebServer {
    private static final WebBankAccount[] accounts=new WebBankAccount[100]; // arrays
    private static int count=0,next=1001;
    private static final Map<String,Integer> sessions=new HashMap<>();
    private static final SecureRandom random=new SecureRandom();
    private static final Path PUBLIC=Path.of("public").toAbsolutePath();
    public static void main(String[] args)throws Exception{
        accounts[count++]=new WebSavingsAccount(next++,"Demo User","1234",500000);
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
        HttpServer server=HttpServer.create(new InetSocketAddress("0.0.0.0",port),0);
        server.createContext("/api/",ATMWebServer::api);
        server.createContext("/",ATMWebServer::staticFile);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(12));server.start();
        System.out.println("ATM web app on port "+port);
    }
    private static synchronized void api(HttpExchange ex)throws IOException{
        ex.getResponseHeaders().set("Cache-Control","no-store");
        try{
            String path=ex.getRequestURI().getPath();
            Map<String,String> form=readForm(ex);
            if(path.equals("/api/register")){
                String name=form.getOrDefault("name","").trim(),pin=form.getOrDefault("pin",""),type=form.getOrDefault("type","Savings");
                if(name.isEmpty()||name.length()>60)throw new IllegalArgumentException("Enter a name (1–60 characters).");
                if(!pin.matches("[0-9]{4}"))throw new IllegalArgumentException("PIN must contain exactly 4 digits.");
                if(!type.equals("Savings")&&!type.equals("Current"))throw new IllegalArgumentException("Choose an account type.");
                if(count>=accounts.length)throw new IllegalArgumentException("Account limit reached.");
                int number=next++;
                accounts[count++]=type.equals("Savings")?new WebSavingsAccount(number,name,pin,0):new WebCurrentAccount(number,name,pin,0);
                respond(ex,200,"{\"ok\":true,\"message\":\"Account created. Number: "+number+"\",\"number\":"+number+"}");return;
            }
            if(path.equals("/api/login")){
                int number=parseNumber(form.get("number"));String pin=form.getOrDefault("pin","");
                WebBankAccount a=find(number);
                if(a==null||!a.pinMatches(pin))throw new IllegalArgumentException("Wrong account number or PIN.");
                byte[] b=new byte[32];random.nextBytes(b);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(b);
                sessions.put(token,number);
                ex.getResponseHeaders().add("Set-Cookie","atm_session="+token+"; HttpOnly; SameSite=Lax; Path=/");
                respond(ex,200,"{\"ok\":true,\"message\":\"Login successful.\"}");return;
            }
            if(path.equals("/api/logout")){
                String token=cookie(ex);sessions.remove(token);ex.getResponseHeaders().add("Set-Cookie","atm_session=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
                respond(ex,200,"{\"ok\":true}");return;
            }
            Integer n=sessions.get(cookie(ex));WebBankAccount a=n==null?null:find(n);
            if(a==null){respond(ex,401,"{\"ok\":false,\"message\":\"Please log in.\"}");return;}
            if(path.equals("/api/me")){respond(ex,200,"{\"ok\":true,\"number\":"+a.number()+",\"name\":\""+esc(a.owner())+"\",\"type\":\""+a.type()+"\",\"balance\":\""+WebBankAccount.money(a.balance())+"\",\"history\":"+history(a.history())+"}");return;}
            if(path.equals("/api/deposit"))a.deposit(amount(form.get("amount")));
            else if(path.equals("/api/withdraw"))a.withdraw(amount(form.get("amount")));
            else if(path.equals("/api/transfer")){
                WebBankAccount receiver=find(parseNumber(form.get("receiver")));long cents=amount(form.get("amount"));
                if(receiver==null||receiver==a)throw new IllegalArgumentException("Invalid receiver account.");
                a.withdraw(cents);receiver.deposit(cents);a.record("Transfer to "+receiver.number());receiver.record("Transfer from "+a.number());
            } else {respond(ex,404,"{\"ok\":false,\"message\":\"Not found\"}");return;}
            respond(ex,200,"{\"ok\":true,\"message\":\"Transaction successful.\"}");
        }catch(IllegalArgumentException|ArithmeticException e){respond(ex,400,"{\"ok\":false,\"message\":\""+esc(e.getMessage())+"\"}");}
        catch(Exception e){respond(ex,500,"{\"ok\":false,\"message\":\"Unexpected server error.\"}");}
    }
    private static long amount(String s){try{java.math.BigDecimal v=new java.math.BigDecimal(s).movePointRight(2);long n=v.longValueExact();if(n<=0)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("Enter a valid positive amount (up to 2 decimals).");}}
    private static int parseNumber(String s){try{return Integer.parseInt(s);}catch(Exception e){throw new IllegalArgumentException("Enter a valid account number.");}}
    private static WebBankAccount find(int n){for(int i=0;i<count;i++)if(accounts[i].number()==n)return accounts[i];return null;}
    private static String history(String[] h){StringJoiner j=new StringJoiner(",","[","]");for(int i=h.length-1;i>=0;i--)j.add("\""+esc(h[i])+"\"");return j.toString();}
    private static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
    private static String cookie(HttpExchange ex){String h=ex.getRequestHeaders().getFirst("Cookie");if(h!=null)for(String p:h.split(";")){p=p.trim();if(p.startsWith("atm_session="))return p.substring(12);}return "";}
    private static Map<String,String> readForm(HttpExchange ex)throws IOException{
        Map<String,String> out=new HashMap<>();
        if(!ex.getRequestMethod().equals("POST"))return out;
        byte[] bytes=ex.getRequestBody().readNBytes(8193);if(bytes.length>8192)throw new IllegalArgumentException("Input too long.");
        for(String pair:new String(bytes,StandardCharsets.UTF_8).split("&")){String[] kv=pair.split("=",2);if(kv.length==2)out.put(URLDecoder.decode(kv[0],StandardCharsets.UTF_8),URLDecoder.decode(kv[1],StandardCharsets.UTF_8));}return out;
    }
    private static void respond(HttpExchange ex,int status,String json)throws IOException{byte[] bytes=json.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(status,bytes.length);try(OutputStream os=ex.getResponseBody()){os.write(bytes);}}
    private static void staticFile(HttpExchange ex)throws IOException{
        String path=ex.getRequestURI().getPath();if(path.equals("/"))path="/index.html";
        Path file=PUBLIC.resolve(path.substring(1)).normalize();if(!file.startsWith(PUBLIC)||!Files.isRegularFile(file)){ex.sendResponseHeaders(404,-1);return;}
        String mime=path.endsWith(".css")?"text/css":path.endsWith(".js")?"text/javascript":"text/html";
        byte[] bytes=Files.readAllBytes(file);ex.getResponseHeaders().set("Content-Type",mime+"; charset=utf-8");ex.sendResponseHeaders(200,bytes.length);try(OutputStream os=ex.getResponseBody()){os.write(bytes);}
    }
}
