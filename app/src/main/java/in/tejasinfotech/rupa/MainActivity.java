package in.tejasinfotech.rupa;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {
    static final String API = "https://rupa.tejasinfotech.in/api/";
    android.content.SharedPreferences prefs;
    LinearLayout root;
    TextView title;
    ProgressDialog progress;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("rupa", MODE_PRIVATE);
        if (prefs.getString("token","").isEmpty()) showLogin(); else showHome();
    }

    TextView tv(String s, int sp) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setPadding(24,20,24,20); return t;
    }
    Button btn(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b;
    }
    void base(String heading) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        title = tv(heading, 24); title.setTextColor(Color.rgb(23,105,170)); title.setTypeface(null,1);
        root.addView(title);
        setContentView(root);
    }
    void showLogin() {
        base("Rupa Crane Service");
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(30,30,30,30);
        EditText user = new EditText(this); user.setHint("Username"); user.setText("admin");
        EditText pass = new EditText(this); pass.setHint("Password"); pass.setInputType(0x81); pass.setText("admin123");
        Button login = btn("LOGIN");
        TextView msg = tv("",15);
        box.addView(user); box.addView(pass); box.addView(login); box.addView(msg); root.addView(box);
        login.setOnClickListener(v -> apiLogin(user.getText().toString(), pass.getText().toString(), msg));
    }
    void apiLogin(String u, String p, TextView msg) {
        progress = ProgressDialog.show(this,"Please wait","Logging in...",true,false);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject(); body.put("username",u); body.put("password",p);
                String out = post("login.php", body.toString(), null);
                JSONObject j = new JSONObject(out);
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (j.optBoolean("ok")) {
                        prefs.edit().putString("token",j.optString("token")).apply();
                        showHome();
                    } else msg.setText(j.optString("message","Login failed"));
                });
            } catch(Exception e) { runOnUiThread(() -> {progress.dismiss(); msg.setText(e.getMessage());}); }
        }).start();
    }
    void showHome() {
        base("Rupa Crane Service");
        root.addView(tv("Dashboard",20));
        Button dash=btn("📊 Dashboard"); Button cust=btn("👥 Customers"); Button prod=btn("🔧 Products / Spare Parts");
        Button docs=btn("📄 Quotations & Invoices"); Button logout=btn("🚪 Logout");
        root.addView(dash); root.addView(cust); root.addView(prod); root.addView(docs); root.addView(logout);
        dash.setOnClickListener(v -> loadDashboard());
        cust.setOnClickListener(v -> loadList("customers.php","Customers"));
        prod.setOnClickListener(v -> loadList("products.php","Products / Spare Parts"));
        docs.setOnClickListener(v -> loadList("documents.php","Quotations & Invoices"));
        logout.setOnClickListener(v -> { prefs.edit().clear().apply(); showLogin(); });
    }
    void loadDashboard() {
        progress=ProgressDialog.show(this,"Loading","Dashboard...",true,false);
        new Thread(() -> {
            try { String out=get("dashboard.php"); JSONObject j=new JSONObject(out);
                runOnUiThread(()->{progress.dismiss(); showText("Dashboard", pretty(j));});
            } catch(Exception e){runOnUiThread(()->{progress.dismiss(); showText("Error",e.getMessage());});}
        }).start();
    }
    void loadList(String endpoint,String heading) {
        progress=ProgressDialog.show(this,"Loading",heading+"...",true,false);
        new Thread(() -> {
            try { String out=get(endpoint); runOnUiThread(()->{progress.dismiss(); showText(heading, prettyJson(out));});}
            catch(Exception e){runOnUiThread(()->{progress.dismiss(); showText("Error",e.getMessage());});}
        }).start();
    }
    void showText(String heading,String text) {
        base(heading);
        ScrollView sv=new ScrollView(this); TextView t=tv(text,16); sv.addView(t); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        Button back=btn("← Back"); back.setOnClickListener(v->showHome()); root.addView(back);
    }
    String pretty(JSONObject j){return j.toString(2);}
    String prettyJson(String s){try{return new JSONArray(s).toString(2);}catch(Exception e){try{return new JSONObject(s).toString(2);}catch(Exception x){return s;}}}

    String post(String path,String body,String token)throws Exception{
        return request(path,"POST",body,token);
    }
    String get(String path)throws Exception{return request(path,"GET",null,prefs.getString("token",""));}

    String request(String path,String method,String body,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API+path).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(20000); c.setReadTimeout(20000);
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("Accept","application/json");
        if(token!=null&&!token.isEmpty()) c.setRequestProperty("Authorization","Bearer "+token);
        if(body!=null){c.setDoOutput(true); try(OutputStream os=c.getOutputStream()){os.write(body.getBytes("UTF-8"));}}
        InputStream is=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();
        BufferedReader r=new BufferedReader(new InputStreamReader(is)); StringBuilder sb=new StringBuilder(); String line;
        while((line=r.readLine())!=null)sb.append(line); c.disconnect(); return sb.toString();
    }
}