package ke.co.romes.timberhardware;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DB db;
    LinearLayout root, content;
    String staff = "";
    int pad = 16;
    static final int BACKUP = 100, RESTORE = 101;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        db.getWritableDatabase();
        login();
    }

    int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(0xff202124);
        t.setPadding(0, dp(7), 0, dp(7));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setInputType(type);
        return e;
    }

    Button button(String s, Runnable r) {
        Button b = new Button(this);
        b.setText(s);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    String money(double n) {
        return String.format(Locale.US, "%,.2f", n);
    }

    String date() {
        return new SimpleDateFormat(
            "yyyy-MM-dd HH:mm", Locale.getDefault()
        ).format(new Date());
    }

    void msg(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(pin.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes)
                out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }

    void login() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(10), dp(20), dp(10));
        box.setOrientation(1);

        TextView title = text("ROMES TIMBER & HARDWARE", 21, true);
        title.setTextColor(0xff183153);
        box.addView(title);

        Spinner users = new Spinner(this);
        String[] names = {"James", "Lucy"};
        users.setAdapter(new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, names));
        box.addView(users);

        EditText pin = field("Enter your PIN", 18);
        box.addView(pin);

        new AlertDialog.Builder(this)
            .setTitle("Staff login")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("Login", (d, w) -> {
                String name = users.getSelectedItem().toString();
                String p = pin.getText().toString();

                Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT pin_hash FROM users WHERE name=?",
                    new String[]{name});

                boolean valid = false;
                if (c.moveToFirst())
                    valid = hash(p).equals(c.getString(0));
                c.close();

                if (valid) {
                    staff = name;
                    showHome();
                } else {
                    msg("Incorrect PIN. Please try again.");
                    login();
                }
            }).show();
    }

    void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(pad), dp(pad), dp(pad), dp(pad));
        root.setBackgroundColor(0xfff5f6f8);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.romes_logo);
        logo.setLayoutParams(new LinearLayout.LayoutParams(
            dp(75), dp(75)));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(logo);

        TextView heading = text("ROMES TIMBER & HARDWARE", 19, true);
        heading.setTextColor(0xff183153);
        root.addView(heading);
        root.addView(text(title + "  |  " + staff, 15, true));

        ScrollView sc = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(1);
        sc.addView(content);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(1);
        nav.addView(button("HOME", this::showHome));
        nav.addView(button("PRODUCTS", this::showProducts));
        nav.addView(button("SALES", this::showSale));
        nav.addView(button("RESTOCK", this::showRestock));
        nav.addView(button("MORE OPTIONS", this::showMore));
        root.addView(nav);

        setContentView(root);
    }

    void showHome() {
        base("Business dashboard");

        Cursor p = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*),COALESCE(SUM(qty),0) FROM products", null);
        p.moveToFirst();
        int products = p.getInt(0);
        int units = p.getInt(1);
        p.close();

        Cursor s = db.getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(total),0)," +
            "COALESCE(SUM((unit_price-unit_cost)*quantity),0) " +
            "FROM sales", null);
        s.moveToFirst();
        double sales = s.getDouble(0);
        double profit = s.getDouble(1);
        s.close();

        content.addView(text("Welcome, " + staff, 19, true));
        content.addView(text("Products: " + products, 17, true));
        content.addView(text("Units in stock: " + units, 17, true));
        content.addView(text("Recorded sales: KES " + money(sales), 17, true));
        content.addView(text("Estimated gross profit/loss: KES " +
            money(profit), 17, true));
        content.addView(text("Shop: Gikomba, Quarry Road", 14, false));
        content.addView(text(
            "Profit is an estimate before expenses and other overheads.",
            13, false));
        content.addView(button("Add product", this::addProduct));
        content.addView(button("View reports", this::reports));
    }

    void showProducts() {
        base("Products and stock");
        content.addView(button("+ Add product", this::addProduct));

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,qty,price,cost FROM products ORDER BY name", null);

        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            int qty = c.getInt(2);
            double price = c.getDouble(3);
            double cost = c.getDouble(4);

            content.addView(text(name, 18, true));
            content.addView(text("Stock: " + qty +
                "\nSelling price: KES " + money(price) +
                "\nEstimated production cost: KES " + money(cost),
                15, false));

            content.addView(button("Edit product", () ->
                editProduct(id, name, qty, price, cost)));

            content.addView(button("Sell this product", () ->
                sellProduct(id, name, price, cost, qty)));

            content.addView(new View(this),
                new LinearLayout.LayoutParams(-1, dp(1)));
        }
        c.close();
    }

    void addProduct() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);

        EditText name = field("Product name", 1);
        EditText qty = field("Opening quantity", 2);
        EditText cost = field("Estimated production cost (KES)", 8194);
        EditText price = field("Normal selling price (KES)", 8194);

        box.addView(name);
        box.addView(qty);
        box.addView(cost);
        box.addView(price);

        new AlertDialog.Builder(this)
            .setTitle("Add product")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    String n = name.getText().toString().trim();
                    int q = Integer.parseInt(qty.getText().toString());
                    double co = Double.parseDouble(cost.getText().toString());
                    double pr = Double.parseDouble(price.getText().toString());

                    if (n.isEmpty() || q < 0 || co < 0 || pr < 0)
                        throw new Exception();

                    ContentValues v = new ContentValues();
                    v.put("name", n);
                    v.put("qty", q);
                    v.put("cost", co);
                    v.put("price", pr);

                    db.getWritableDatabase().insertOrThrow(
                        "products", null, v);
                    showProducts();
                } catch (Exception e) {
                    msg("Please enter valid product details.");
                }
            }).show();
    }

    void editProduct(int id, String name, int qty,
                     double price, double cost) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);

        EditText n = field("Product name", 1);
        n.setText(name);
        EditText q = field("Stock quantity", 2);
        q.setText("" + qty);
        EditText p = field("Selling price (KES)", 8194);
        p.setText("" + price);
        EditText co = field("Production cost (KES)", 8194);
        co.setText("" + cost);

        box.addView(n);
        box.addView(q);
        box.addView(p);
        box.addView(co);

        new AlertDialog.Builder(this)
            .setTitle("Update product")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    String newName = n.getText().toString().trim();
                    int newQty = Integer.parseInt(q.getText().toString());
                    double newPrice = Double.parseDouble(p.getText().toString());
                    double newCost = Double.parseDouble(co.getText().toString());

                    if (newName.isEmpty() || newQty < 0 ||
                        newPrice < 0 || newCost < 0)
                        throw new Exception();

                    ContentValues v = new ContentValues();
                    v.put("name", newName);
                    v.put("qty", newQty);
                    v.put("price", newPrice);
                    v.put("cost", newCost);

                    db.getWritableDatabase().update(
                        "products", v, "id=?", new String[]{"" + id});
                    showProducts();
                } catch (Exception e) {
                    msg("Please enter valid details.");
                }
            }).show();
    }

    void showSale() {
        base("Record a sale");
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,qty,price,cost FROM products ORDER BY name", null);

        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            int qty = c.getInt(2);
            double price = c.getDouble(3);
            double cost = c.getDouble(4);

            content.addView(text(name + " — " + qty +
                " in stock — KES " + money(price), 16, true));
            content.addView(button("Record sale", () ->
                sellProduct(id, name, price, cost, qty)));
        }
        c.close();
    }

    void sellProduct(int id, String name, double normalPrice,
                     double cost, int available) {
        if (available <= 0) {
            msg("No stock available.");
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        EditText q = field("Quantity to sell", 2);
        EditText price = field("Actual price per item (KES)", 8194);
        price.setText("" + normalPrice);
        EditText customer = field("Customer name (optional)", 1);

        box.addView(q);
        box.addView(price);
        box.addView(customer);

        new AlertDialog.Builder(this)
            .setTitle("Sell: " + name)
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Record sale", (d, w) -> {
                try {
                    int quantity = Integer.parseInt(q.getText().toString());
                    double selling = Double.parseDouble(price.getText().toString());

                    if (quantity <= 0 || quantity > available || selling < 0)
                        throw new Exception();

                    double total = quantity * selling;
                    double profit = quantity * (selling - cost);
                    SQLiteDatabase database = db.getWritableDatabase();
                    database.beginTransaction();

                    try {
                        ContentValues stock = new ContentValues();
                        stock.put("qty", available - quantity);
                        database.update("products", stock,
                            "id=?", new String[]{"" + id});

                        ContentValues sale = new ContentValues();
                        sale.put("product_id", id);
                        sale.put("product_name", name);
                        sale.put("quantity", quantity);
                        sale.put("total", total);
                        sale.put("unit_price", selling);
                        sale.put("unit_cost", cost);
                        sale.put("customer", customer.getText().toString());
                        sale.put("staff", staff);
                        sale.put("date", date());
                        long saleId = database.insertOrThrow("sales", null, sale);

                        database.setTransactionSuccessful();

                        String receipt =
                            "ROMES TIMBER & HARDWARE\n" +
                            "Gikomba, Quarry Road\n" +
                            "Receipt No: " + saleId + "\n" +
                            "Date: " + date() + "\n" +
                            "Served by: " + staff + "\n" +
                            "Customer: " + customer.getText().toString() + "\n\n" +
                            name + "\nQuantity: " + quantity +
                            "\nPrice each: KES " + money(selling) +
                            "\nTOTAL: KES " + money(total) +
                            "\n\nThank you for shopping with us.";

                        new AlertDialog.Builder(this)
                            .setTitle("Sale recorded")
                            .setMessage(receipt +
                                "\n\nEstimated " +
                                (profit >= 0 ? "profit: " : "loss: ") +
                                "KES " + money(Math.abs(profit)))
                            .setPositiveButton("Done", (a, b) -> showHome())
                            .setNeutralButton("Share receipt", (a, b) -> {
                                Intent send = new Intent(Intent.ACTION_SEND);
                                send.setType("text/plain");
                                send.putExtra(Intent.EXTRA_TEXT, receipt);
                                startActivity(Intent.createChooser(
                                    send, "Share receipt"));
                                showHome();
                            }).show();
                    } finally {
                        database.endTransaction();
                    }
                } catch (Exception e) {
                    msg("Please enter a valid quantity and price.");
                }
            }).show();
    }

    void showRestock() {
        base("Restock products");
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,qty FROM products ORDER BY name", null);

        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            int qty = c.getInt(2);

            content.addView(text(name + " — Stock: " + qty, 16, true));
            content.addView(button("Add stock", () -> {
                EditText amount = field("Quantity to add", 2);
                new AlertDialog.Builder(this)
                    .setTitle("Restock " + name)
                    .setView(amount)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save", (d, w) -> {
                        try {
                            int add = Integer.parseInt(amount.getText().toString());
                            if (add <= 0) throw new Exception();

                            ContentValues v = new ContentValues();
                            v.put("qty", qty + add);
                            db.getWritableDatabase().update(
                                "products", v, "id=?",
                                new String[]{"" + id});
                            showRestock();
                        } catch (Exception e) {
                            msg("Enter a positive quantity.");
                        }
                    }).show();
            }));
        }
        c.close();
    }

    void showMore() {
        base("More business tools");
        content.addView(button("Customers", () -> people("customers")));
        content.addView(button("Suppliers", () -> people("suppliers")));
        content.addView(button("Record an expense", this::addExpense));
        content.addView(button("Reports", this::reports));
        content.addView(button("Change my PIN", this::changePin));
        content.addView(button("Backup business data", () ->
            startActivityForResult(
                new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .setType("application/octet-stream")
                    .putExtra(Intent.EXTRA_TITLE, "ROMES-backup.db"), BACKUP)));
        content.addView(button("Restore business backup", () ->
            startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE), RESTORE)));
        content.addView(button("Sign out", () -> {
            staff = "";
            login();
        }));
    }

    void people(String table) {
        base(table.equals("customers") ? "Customers" : "Suppliers");
        content.addView(button("Add new", () -> {
            EditText name = field("Name", 1);
            new AlertDialog.Builder(this)
                .setTitle("Add " + table.substring(0, table.length()-1))
                .setView(name)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        msg("Enter a name.");
                        return;
                    }
                    ContentValues v = new ContentValues();
                    v.put("name", n);
                    db.getWritableDatabase().insert(table, null, v);
                    people(table);
                }).show();
        }));

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT name FROM " + table + " ORDER BY name", null);
        while (c.moveToNext())
            content.addView(text(c.getString(0), 16, false));
        c.close();
    }

    void addExpense() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        EditText description = field("Expense description", 1);
        EditText amount = field("Amount (KES)", 8194);
        box.addView(description);
        box.addView(amount);

        new AlertDialog.Builder(this)
            .setTitle("Record expense")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    String desc = description.getText().toString().trim();
                    double value = Double.parseDouble(amount.getText().toString());
                    if (desc.isEmpty() || value <= 0) throw new Exception();

                    ContentValues v = new ContentValues();
                    v.put("description", desc);
                    v.put("amount", value);
                    v.put("staff", staff);
                    v.put("date", date());
                    db.getWritableDatabase().insertOrThrow("expenses", null, v);
                    msg("Expense saved.");
                    showMore();
                } catch (Exception e) {
                    msg("Enter a description and valid amount.");
                }
            }).show();
    }

    void reports() {
        base("Business reports");

        Cursor s = db.getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(total),0)," +
            "COALESCE(SUM((unit_price-unit_cost)*quantity),0),COUNT(*) " +
            "FROM sales", null);
        s.moveToFirst();
        double sales = s.getDouble(0);
        double gross = s.getDouble(1);
        int transactions = s.getInt(2);
        s.close();

        Cursor e = db.getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM expenses", null);
        e.moveToFirst();
        double expenses = e.getDouble(0);
        e.close();

        content.addView(text("Total recorded sales: KES " +
            money(sales), 17, true));
        content.addView(text("Number of sales: " + transactions, 16, false));
        content.addView(text("Estimated gross profit/loss: KES " +
            money(gross), 16, true));
        content.addView(text("Recorded expenses: KES " +
            money(expenses), 16, false));
        content.addView(text("Estimated result after recorded expenses: KES " +
            money(gross - expenses), 17, true));
        content.addView(text(
            "Estimates only. Unrecorded overheads, taxes and other costs are excluded.",
            13, false));
    }

    void changePin() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        EditText oldPin = field("Current PIN", 18);
        EditText newPin = field("New PIN (at least 4 digits)", 18);
        box.addView(oldPin);
        box.addView(newPin);

        new AlertDialog.Builder(this)
            .setTitle("Change PIN")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update", (d, w) -> {
                String old = oldPin.getText().toString();
                String fresh = newPin.getText().toString();

                Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT pin_hash FROM users WHERE name=?",
                    new String[]{staff});
                boolean valid = c.moveToFirst() &&
                    hash(old).equals(c.getString(0));
                c.close();

                if (!valid || fresh.length() < 4) {
                    msg("Current PIN is wrong or new PIN is too short.");
                    return;
                }

                ContentValues v = new ContentValues();
                v.put("pin_hash", hash(fresh));
                db.getWritableDatabase().update(
                    "users", v, "name=?", new String[]{staff});
                msg("PIN changed successfully.");
            }).show();
    }

    void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;

        try {
            Uri uri = data.getData();
            if (request == BACKUP) {
                db.close();
                File source = getDatabasePath("romes_v3.db");
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) > 0)
                        out.write(buffer, 0, n);
                }
                db = new DB(this);
                msg("Backup saved.");
            } else if (request == RESTORE) {
                db.close();
                File target = getDatabasePath("romes_v3.db");
                File parent = target.getParentFile();
                if (parent != null) parent.mkdirs();

                new File(target.getPath() + "-wal").delete();
                new File(target.getPath() + "-shm").delete();

                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) > 0)
                        out.write(buffer, 0, n);
                }
                db = new DB(this);
                db.getWritableDatabase();
                msg("Backup restored. Please sign in again.");
                staff = "";
                login();
            }
        } catch (Exception e) {
            db = new DB(this);
            msg("Backup operation failed: " + e.getMessage());
        }
    }

    static class DB extends SQLiteOpenHelper {
        DB(Context c) {
            super(c, "romes_v3.db", null, 1);
        }

        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE products(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL,qty INTEGER NOT NULL," +
                "price REAL NOT NULL,cost REAL NOT NULL)");

            d.execSQL("CREATE TABLE sales(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER,product_name TEXT,quantity INTEGER," +
                "total REAL,unit_price REAL,unit_cost REAL," +
                "customer TEXT,staff TEXT,date TEXT)");

            d.execSQL("CREATE TABLE users(" +
                "name TEXT PRIMARY KEY,pin_hash TEXT NOT NULL)");

            d.execSQL("CREATE TABLE customers(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");

            d.execSQL("CREATE TABLE suppliers(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");

            d.execSQL("CREATE TABLE expenses(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "description TEXT,amount REAL,staff TEXT,date TEXT)");

            ContentValues james = new ContentValues();
            james.put("name", "James");
            james.put("pin_hash", new MainActivity().hash("1234"));
            d.insert("users", null, james);

            ContentValues lucy = new ContentValues();
            lucy.put("name", "Lucy");
            lucy.put("pin_hash", new MainActivity().hash("5678"));
            d.insert("users", null, lucy);
        }

        public void onUpgrade(SQLiteDatabase d, int oldVersion,
                              int newVersion) {
        }
    }
}
