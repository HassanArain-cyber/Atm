// Scanner user se keyboard input lene ke liye use hota hai.
import java.util.Scanner;

// Abstraction: common structure for every bank account.
// ABSTRACT CLASS: Is class ka direct object nahi banta.
// SavingsAccount aur CurrentAccount isko extend karte hain.
abstract class BankAccount {
    // Encapsulation: account details cannot be changed directly.
    // private fields ko class ke bahar directly access nahi kar sakte.
    private final int accountNumber;
    private final String owner;
    private final int pin;
    private double balance;
    // ARRAY: Aakhri 50 transactions ko memory mein store karta hai.
    private final String[] history = new String[50]; // Array
    private int historyCount = 0;

    // Constructor
    // CONSTRUCTOR: Naya account bante waqt initial values set karta hai.
    public BankAccount(int accountNumber, String owner, int pin, double openingBalance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.pin = pin;
        this.balance = openingBalance;
        addHistory("Account opened: Rs. " + openingBalance);
    }

    // GETTER METHODS: Private data ko controlled tareeqe se read karte hain.
    public int getAccountNumber() { return accountNumber; }
    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
    public boolean checkPin(int enteredPin) { return pin == enteredPin; }

    // METHOD + IF-ELSE + OPERATOR: Valid deposit ko balance mein add karo.
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount; // Operator
            addHistory("Deposited: Rs. " + amount);
            System.out.printf("Deposited! Balance: Rs. %.2f%n", balance);
        } else {
            System.out.println("Amount must be greater than zero.");
        }
    }

    // IF-ELSE: Pehle amount aur balance check hota hai, phir withdrawal.
    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("Enter a positive amount.");
        } else if (amount > balance) {
            System.out.println("Insufficient balance.");
        } else {
            balance -= amount;
            addHistory("Withdrawn: Rs. " + amount);
            System.out.printf("Withdrawn! Balance: Rs. %.2f%n", balance);
        }
    }

    // ARRAY + LOOP: History full ho to purani entry remove karke nayi add karo.
    public void addHistory(String message) {
        if (historyCount < history.length) {
            history[historyCount++] = message;
        } else {
            for (int i = 1; i < history.length; i++) {
                history[i - 1] = history[i];
            }
            history[history.length - 1] = message;
        }
    }

    // FOR LOOP: Stored transactions ko ek ek karke print karta hai.
    public void showHistory() {
        System.out.println("\n--- Transaction History ---");
        for (int i = 0; i < historyCount; i++) { // Loop + array
            System.out.println((i + 1) + ". " + history[i]);
        }
    }

    // ABSTRACT METHOD: Har child class apna account type display karegi.
    public abstract void showAccountType(); // Abstraction
}

// Inheritance: SavingsAccount inherits BankAccount.
// INHERITANCE: extends se parent BankAccount ki methods milti hain.
class SavingsAccount extends BankAccount {
    public SavingsAccount(int number, String owner, int pin, double balance) {
        // super() parent class ka constructor call karta hai.
        super(number, owner, pin, balance); // Parent constructor
    }

    @Override // Polymorphism: child provides its own implementation.
    public void showAccountType() {
        System.out.println("Account Type: Savings Account");
    }
}

// Dusri child class: Same parent, lekin apna account type.
class CurrentAccount extends BankAccount {
    public CurrentAccount(int number, String owner, int pin, double balance) {
        super(number, owner, pin, balance);
    }

    @Override // Polymorphism
    public void showAccountType() {
        System.out.println("Account Type: Current Account");
    }
}

// MAIN CLASS: Program yahan se start hota hai.
public class ATMSystem {
    private static final Scanner input = new Scanner(System.in);
    // POLYMORPHISM: Parent-type array dono child classes ke objects rakh sakta hai.
    private static final BankAccount[] accounts = new BankAccount[20]; // Array of accounts
    private static int accountCount = 0;
    private static int nextAccountNumber = 1001;

    public static void main(String[] args) {
        // Demo account: number 1001, PIN 1234
        accounts[accountCount++] = new SavingsAccount(nextAccountNumber++, "Demo User", 1234, 5000);

        // WHILE LOOP: Exit choose karne tak main menu repeat hota rahega.
        while (true) { // Loop
            System.out.println("\n====== JAVA ATM SYSTEM ======");
            System.out.println("1. Create Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            int choice = readInt("Choose: ");

            if (choice == 1) createAccount();
            else if (choice == 2) login();
            else if (choice == 3) {
                System.out.println("Thank you for using our ATM!");
                break;
            } else System.out.println("Invalid choice.");
        }
    }

    // Naya account banata hai; name, PIN aur account type validate karta hai.
    private static void createAccount() {
        if (accountCount >= accounts.length) {
            System.out.println("Maximum number of accounts reached.");
            return;
        }
        System.out.print("Enter your name: ");
        String name = input.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        int pin = readInt("Choose a 4-digit PIN: ");
        if (pin < 1000 || pin > 9999) {
            System.out.println("PIN must contain exactly 4 digits.");
            return;
        }
        System.out.println("1. Savings  2. Current");
        int type = readInt("Account type: ");
        if (type != 1 && type != 2) {
            System.out.println("Invalid account type.");
            return;
        }
        int number = nextAccountNumber++;
        BankAccount account;
        if (type == 1) account = new SavingsAccount(number, name, pin, 0);
        else account = new CurrentAccount(number, name, pin, 0);
        accounts[accountCount++] = account;
        System.out.println("Account created! Your account number: " + number);
    }

    // LOGIN: Account number aur PIN sahi hon to ATM menu khulta hai.
    private static void login() {
        int number = readInt("Account number: ");
        int pin = readInt("PIN: ");
        BankAccount account = findAccount(number);
        if (account == null || !account.checkPin(pin)) {
            System.out.println("Wrong account number or PIN.");
            return;
        }
        System.out.println("Welcome, " + account.getOwner() + "!");
        // RUNTIME POLYMORPHISM: Object ke hisaab se overridden method chalega.
        account.showAccountType(); // Runtime polymorphism
        accountMenu(account);
    }

    // FOR LOOP + ARRAY: Account number ko stored accounts mein search karta hai.
    private static BankAccount findAccount(int number) {
        for (int i = 0; i < accountCount; i++) {
            if (accounts[i].getAccountNumber() == number) return accounts[i];
        }
        return null;
    }

    // Login ke baad options repeat hote hain jab tak user logout na kare.
    private static void accountMenu(BankAccount account) {
        while (true) {
            System.out.println("\n------ ATM MENU ------");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Transfer Money");
            System.out.println("5. Transaction History");
            System.out.println("6. Logout");
            int choice = readInt("Choose: ");

            if (choice == 1) System.out.printf("Balance: Rs. %.2f%n", account.getBalance());
            else if (choice == 2) account.deposit(readAmount("Deposit amount: Rs. "));
            else if (choice == 3) account.withdraw(readAmount("Withdraw amount: Rs. "));
            else if (choice == 4) transfer(account);
            else if (choice == 5) account.showHistory();
            else if (choice == 6) {
                System.out.println("Logged out.");
                return;
            } else System.out.println("Invalid choice.");
        }
    }

    // TRANSFER: Receiver aur balance check karke paisay transfer karta hai.
    private static void transfer(BankAccount sender) {
        int receiverNumber = readInt("Receiver account number: ");
        BankAccount receiver = findAccount(receiverNumber);
        if (receiver == null || receiver == sender) {
            System.out.println("Invalid receiver account.");
            return;
        }
        double amount = readAmount("Transfer amount: Rs. ");
        if (amount <= 0 || !Double.isFinite(amount)) {
            System.out.println("Amount must be positive.");
        } else if (amount > sender.getBalance()) {
            System.out.println("Insufficient balance.");
        } else {
            // Reuse the existing methods for safe balance changes.
            sender.withdraw(amount);
            receiver.deposit(amount);
            sender.addHistory("Transfer sent to account " + receiverNumber);
            receiver.addHistory("Transfer received from account " + sender.getAccountNumber());
            System.out.println("Transfer successful!");
        }
    }

    // INPUT METHOD: Galat integer par crash hone ke bajaye dobara poochta hai.
    private static int readInt(String message) {
        while (true) {
            System.out.print(message);
            try { return Integer.parseInt(input.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Enter a valid whole number."); }
        }
    }

    // INPUT METHOD: Decimal amount leta hai aur invalid values reject karta hai.
    private static double readAmount(String message) {
        while (true) {
            System.out.print(message);
            try {
                double amount = Double.parseDouble(input.nextLine().trim());
                if (Double.isFinite(amount)) return amount;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a valid amount.");
        }
    }
}
