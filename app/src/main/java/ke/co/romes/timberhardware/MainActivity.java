package ke.co.romes.timberhardware;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DB db; LinearLayout root, content; TextView summary;
    int pad = 18;
    public void onCreate(Bundle b) {
        super.onCreate(b); db = new DB(this); showHome();
    }
    int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density); }
    TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(0xff202124); t.setPadding(0,dp(7),0,dp(7));
        if(bold)t.setTypeface(null,Typeface.BOLD); return t;
    }
    Button button(String s, Runnable r) {
        Button b = new Button(this); b.setText(s); b.setOnClickListener(v->r.run()); return b;
    }
    EditText field(String hint, int type) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setInputType(type);
        return e;
    }
    void base(String title) {
        root = new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(pad),dp(pad),dp(pad),dp(pad));
        root.setBackgroundColor(0xfff5f6f8);
        TextView h=text("ROMES TIMBER & HARDWARE",20,true); h.setTextColor(0xff183153); root.addView(h);
        root.addView(text(title,17,true));
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(1);
        sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(0);
        nav.addView(button("Home",this::showHome),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("Products",this::showProducts),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("Sale",this::showSale),new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(button("Restock",this::showRestock),new LinearLayout.LayoutParams(0,-2,1));
        root.addView(nav); setContentView(root);
    }
    void showHome() {
        base("Business dashboard");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*), COALESCE(SUM(qty),0) FROM products",null);
        c.moveToFirst(); int count=c.getInt(0), units=c.getInt(1); c.close();
        Cursor s=db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM sales",null);
        s.moveToFirst(); double sales=s.getDouble(0); s.close();
        content.addView(text("Welcome. Manage your shop from here.",16,false));
        content.addView(text("Products: "+count,18,true));
        content.addView(text("Units in stock: "+units,18,true));
        content.addView(text("Recorded sales: KES "+money(sales),18,true));
        content.addView(text("Shop: Gikomba, along Quarry Road",14,false));
        content.addView(text("Start by adding your products and opening stock.",14,false));
        content.addView(button("Add a product",this::addProduct));
    }
    String money(double n){return String.format(Locale.US,"%,.2f",n);}
    void showProducts() {
        base("Products & stock");
        content.addView(button("+ Add product",this::addProduct));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,qty,price FROM products ORDER BY name",null);
        while(c.moveToNext()){
            final int id=c.getInt(0); String name=c.getString(1); int qty=c.getInt(2); double price=c.getDouble(3);
            content.addView(text(name+"\\nStock: "+qty+"    Selling price: KES "+money(price),16,true));
            content.addView(button("Sell this product",()->sellProduct(id,name,price,qty)));
            View line=new View(this); line.setBackgroundColor(0xffdddddd); content.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        } c.close();
    }
    void addProduct() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(1);
        EditText n=field("Product name (e.g. Flush door)",1);
        EditText q=field("Opening quantity",2); EditText p=field("Selling price (KES)",8194);
        box.addView(n);box.addView(q);box.addView(p);
        new AlertDialog.Builder(this).setTitle("Add product").setView(box)
          .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            try { if(n.getText().toString().trim().isEmpty())throw new Exception();
              ContentValues v=new ContentValues();v.put("name",n.getText().toString().trim());
              v.put("qty",Integer.parseInt(q.getText().toString()));v.put("price",Double.parseDouble(p.getText().toString()));
              db.getWritableDatabase().insertOrThrow("products",null,v); showProducts();
            }catch(Exception e){msg("Enter a name, quantity and valid price.");}
          }).show();
    }
    void showSale() {
        base("Record a sale");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,qty,price FROM products ORDER BY name",null);
        while(c.moveToNext()){
            int id=c.getInt(0), qty=c.getInt(2); String name=c.getString(1); double price=c.getDouble(3);
            content.addView(text(name+" — "+qty+" in stock — KES "+money(price),15,true));
            content.addView(button("Record sale",()->sellProduct(id,name,price,qty)));
        } c.close();
    }
    void sellProduct(int id,String name,double price,int available) {
        if(available<=0){msg("No stock available.");return;}
        EditText amount=field("Quantity to sell (max "+available+")",2);
        new AlertDialog.Builder(this).setTitle("Sell: "+name).setView(amount)
        .setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->{
            try{
                int q=Integer.parseInt(amount.getText().toString()); if(q<=0||q>available)throw new Exception();
                ContentValues v=new ContentValues();v.put("qty",available-q);
                db.getWritableDatabase().update("products",v,"id=?",new String[]{""+id});
                ContentValues sale=new ContentValues();sale.put("product_id",id);sale.put("quantity",q);
                sale.put("total",q*price);sale.put("date",new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date()));
                db.getWritableDatabase().insert("sales",null,sale);
                new AlertDialog.Builder(this).setTitle("Sale recorded")
                 .setMessage(name+"\\nQuantity: "+q+"\\nTotal: KES "+money(q*price))
                 .setPositiveButton("OK",(x,y)->showHome()).show();
            }catch(Exception ex){msg("Enter a valid quantity within available stock.");}
        }).show();
    }
    void showRestock() {
        base("Restock products");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,qty FROM products ORDER BY name",null);
        while(c.moveToNext()){
            int id=c.getInt(0), qty=c.getInt(2); String name=c.getString(1);
            content.addView(text(name+" — current stock: "+qty,15,true));
            content.addView(button("Add stock",()->{
                EditText q=field("Quantity to add",2);
                new AlertDialog.Builder(this).setTitle("Restock "+name).setView(q)
                .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                    try{int add=Integer.parseInt(q.getText().toString());if(add<=0)throw new Exception();
                    ContentValues v=new ContentValues();v.put("qty",qty+add);
                    db.getWritableDatabase().update("products",v,"id=?",new String[]{""+id});showRestock();
                    }catch(Exception e){msg("Enter a positive quantity.");}
                }).show();
            }));
        } c.close();
    }
    void msg(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"romes_shop.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,qty INTEGER NOT NULL,price REAL NOT NULL)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER,quantity INTEGER,total REAL,date TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
    }
}
