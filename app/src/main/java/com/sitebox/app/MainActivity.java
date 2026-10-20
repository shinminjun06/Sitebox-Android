package com.sitebox.app;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.util.*;
public class MainActivity extends Activity {
 JSONArray sites=new JSONArray(); LinearLayout list; String filter="";
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 void load(){try{sites=new JSONArray(getPreferences(0).getString("sites","[]"));}catch(Exception e){sites=new JSONArray();}}
 void save(){getPreferences(0).edit().putString("sites",sites.toString()).apply();}
 @Override public void onCreate(Bundle b){super.onCreate(b);load();draw();}
 void draw(){LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(16),dp(16),dp(8));root.setBackgroundColor(Color.WHITE);
 TextView title=new TextView(this);title.setText("SiteBox");title.setTextSize(26);title.setTextColor(Color.BLACK);root.addView(title);
 Button add=new Button(this);add.setText("+ Add website");add.setOnClickListener(v->edit(-1));root.addView(add);
 EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search websites");search.setText(filter);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filter=s.toString();populate();}public void afterTextChanged(Editable e){}});root.addView(search);
 ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);populate();}
 void populate(){if(list==null)return;list.removeAllViews();ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<sites.length();i++)order.add(i);Collections.sort(order,(a,b)->Boolean.compare(sites.optJSONObject(b).optBoolean("fav"),sites.optJSONObject(a).optBoolean("fav")));
 for(int idx:order){JSONObject item=sites.optJSONObject(idx);if(item==null)continue;String name=item.optString("name"),url=item.optString("url"),cat=item.optString("category");if(!(name+" "+url+" "+cat).toLowerCase().contains(filter.toLowerCase()))continue;
 LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(0,dp(9),0,dp(9));
 Button open=new Button(this);open.setAllCaps(false);open.setText((item.optBoolean("fav")?"★ ":"☆ ")+name+"\n"+cat+" · "+url);open.setOnClickListener(v->{try{String target=url.matches("(?i)^https?://.*")?url:"https://"+url;startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(target)));}catch(Exception ex){Toast.makeText(this,"Cannot open URL",Toast.LENGTH_SHORT).show();}});row.addView(open);
 LinearLayout actions=new LinearLayout(this);Button fav=new Button(this);fav.setText(item.optBoolean("fav")?"Unfavorite":"Favorite");fav.setOnClickListener(v->{try{item.put("fav",!item.optBoolean("fav"));save();populate();}catch(Exception e){}});actions.addView(fav,new LinearLayout.LayoutParams(0,-2,1));
 Button edit=new Button(this);edit.setText("Edit");edit.setOnClickListener(v->edit(idx));actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1));
 Button del=new Button(this);del.setText("Delete");del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Delete "+name+"?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{sites.remove(idx);save();populate();}).show());actions.addView(del,new LinearLayout.LayoutParams(0,-2,1));
 row.addView(actions);list.addView(row);
 }}
 void edit(int index){JSONObject old=index<0?new JSONObject():sites.optJSONObject(index);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(18),0,dp(18),0);fields.setOrientation(1);
 EditText name=new EditText(this);name.setHint("Website name");name.setSingleLine();name.setText(old.optString("name"));fields.addView(name);
 EditText url=new EditText(this);url.setHint("URL (https://example.com)");url.setSingleLine();url.setInputType(17);url.setText(old.optString("url"));fields.addView(url);
 EditText cat=new EditText(this);cat.setHint("Category");cat.setSingleLine();cat.setText(old.optString("category"));fields.addView(cat);
 new AlertDialog.Builder(this).setTitle(index<0?"Add website":"Edit website").setView(fields).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String u=url.getText().toString().trim();String n=name.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(this,"Name and URL required",Toast.LENGTH_SHORT).show();return;}if(!u.matches("(?i)^https?://.*"))u="https://"+u;try{JSONObject item=new JSONObject();item.put("name",n);item.put("url",u);item.put("category",cat.getText().toString().trim());item.put("fav",old.optBoolean("fav"));if(index<0)sites.put(item);else sites.put(index,item);save();populate();}catch(Exception e){}}).show();}
}
