package com.sitebox.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
 JSONArray sites=new JSONArray();
 LinearLayout shelves;
 EditText search;
 String filter=""; int page=0;
 final int[] covers={0xFF315B57,0xFF8C493E,0xFF334C70,0xFF755A84,0xFFAD783A,0xFF566B42,0xFF5F4149,0xFF476B78};
 int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 void load(){try{sites=new JSONArray(getPreferences(0).getString("sites","[]"));}catch(Exception e){sites=new JSONArray();}}
 void save(){getPreferences(0).edit().putString("sites",sites.toString()).apply();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(47,29,21));getWindow().setNavigationBarColor(Color.rgb(47,29,21));load();draw();}
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 void draw(){
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xFF281B16);
 LinearLayout header=new LinearLayout(this);header.setOrientation(1);header.setPadding(dp(20),dp(22),dp(20),dp(12));header.setBackgroundColor(0xFF34231B);
 TextView eyebrow=label("YOUR PERSONAL LIBRARY",11,0xFFC9A978);eyebrow.setLetterSpacing(.19f);header.addView(eyebrow);
 LinearLayout headline=new LinearLayout(this);headline.setGravity(Gravity.CENTER_VERTICAL);
 TextView title=label("SiteBox  ·  서재",27,0xFFF5E8D1);title.setTypeface(Typeface.SERIF,Typeface.BOLD);headline.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 TextView add=label("＋",30,0xFFF8EBD5);add.setGravity(Gravity.CENTER);add.setBackground(bg(0xFF775135,14));headline.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));add.setOnClickListener(v->edit(-1));
 header.addView(headline);
 search=new EditText(this);search.setSingleLine(true);search.setTextColor(0xFFF5E8D1);search.setHintTextColor(0xFFBEA994);search.setTextSize(15);search.setHint("⌕  책 제목 · 주소 · 카테고리 검색");search.setPadding(dp(14),0,dp(12),0);search.setBackground(bg(0xFF4D382B,12));search.setText(filter);
 LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(46));sp.topMargin=dp(12);header.addView(search,sp);root.addView(header);
 ScrollView sc=new ScrollView(this);sc.setFillViewport(true);shelves=new LinearLayout(this);shelves.setOrientation(1);shelves.setPadding(dp(12),dp(14),dp(12),dp(25));shelves.setBackgroundColor(0xFF38271E);sc.addView(shelves);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
 search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){filter=s.toString();page=0;populate();}public void afterTextChanged(Editable e){}});
 populate();
 }
 void populate(){
 shelves.removeAllViews();
 ArrayList<Integer> ids=new ArrayList<>();
 for(int i=0;i<sites.length();i++){JSONObject o=sites.optJSONObject(i);if(o==null)continue;String all=o.optString("name")+" "+o.optString("url")+" "+o.optString("category");if(all.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))ids.add(i);}
 Collections.sort(ids,(a,b)->Boolean.compare(sites.optJSONObject(b).optBoolean("fav"),sites.optJSONObject(a).optBoolean("fav")));
 int pages=Math.max(1,(ids.size()+62)/63);page=Math.max(0,Math.min(page,pages-1));
 LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
 TextView prev=label("◀",19,0xFFF3D7B0);prev.setGravity(Gravity.CENTER);nav.addView(prev,new LinearLayout.LayoutParams(dp(45),dp(42)));prev.setOnClickListener(v->{if(page>0){page--;populate();}});
 TextView count=label("나무 책장  "+(page+1)+" / "+pages+"   ·   "+ids.size()+"권",14,0xFFE9D2B0);count.setGravity(Gravity.CENTER);nav.addView(count,new LinearLayout.LayoutParams(0,dp(42),1));
 TextView next=label("▶",19,0xFFF3D7B0);next.setGravity(Gravity.CENTER);nav.addView(next,new LinearLayout.LayoutParams(dp(45),dp(42)));next.setOnClickListener(v->{if(page<pages-1){page++;populate();}});
 shelves.addView(nav);
 for(int shelf=0;shelf<9;shelf++){
 LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.BOTTOM);row.setPadding(dp(4),dp(10),dp(4),0);
 GradientDrawable back=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFF24150F,0xFF543929,0xFF291911});back.setStroke(dp(3),0xFF805334);row.setBackground(back);
 for(int k=0;k<7;k++){
 int pos=page*63+shelf*7+k;
 LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(112),1);p.setMargins(dp(1),0,dp(1),0);
 if(pos<ids.size()){int idx=ids.get(pos);row.addView(new BookView(this,sites.optJSONObject(idx),idx),p);}
 else {View blank=new View(this);row.addView(blank,p);}
 }
 shelves.addView(row);
 View lip=new View(this);GradientDrawable wood=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFD09A5E,0xFF986036,0xFF4D2D1D});wood.setStroke(dp(1),0xFFBD8954);lip.setBackground(wood);shelves.addView(lip,new LinearLayout.LayoutParams(-1,dp(15)));
 View shadow=new View(this);shadow.setBackgroundColor(0xFF21140F);shelves.addView(shadow,new LinearLayout.LayoutParams(-1,dp(6)));
 }
 TextView tip=label("책을 누르면 사이트 열기  ·  길게 누르면 편집 / 즐겨찾기",12,0xFFCBB394);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(18),0,dp(8));shelves.addView(tip);
 }
 class BookView extends View {
 Paint p=new Paint(3);JSONObject item;int idx;long down;float y;
 BookView(Context c,JSONObject obj,int index){super(c);item=obj;idx=index;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription(obj.optString("name"));}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
 float w=getWidth(),h=getHeight(),left=dp(1),top=dp(5),right=w-dp(1),bottom=h;
 int color=covers[(item.optString("name").hashCode()&0x7fffffff)%covers.length];
 p.setShadowLayer(dp(2),dp(1),dp(1),0xBB000000);p.setColor(0xFF100C0A);canvas.drawRoundRect(left+dp(2),top+dp(2),right,bottom,dp(2),dp(2),p);p.clearShadowLayer();
 p.setColor(color);canvas.drawRoundRect(left,top,right-dp(2),bottom-dp(1),dp(2),dp(2),p);
 p.setColor(0x66000000);canvas.drawRect(left+dp(3),top,right-dp(8),bottom-dp(1),p);
 p.setColor(0xFFE1B980);canvas.drawRect(left+dp(4),top+dp(9),right-dp(6),top+dp(11),p);canvas.drawRect(left+dp(4),bottom-dp(10),right-dp(6),bottom-dp(8),p);
 canvas.save();canvas.rotate(-90,(left+right)/2,(top+bottom)/2);
 p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(10));p.setColor(0xFFF9E6C6);
 String name=item.optString("name","Site");if(name.length()>14)name=name.substring(0,13)+"…";
 canvas.drawText(name,(left+right)/2,(top+bottom)/2+dp(3),p);canvas.restore();
 if(item.optBoolean("fav")){p.setTextSize(dp(11));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFFD477);canvas.drawText("★",w/2,top+dp(24),p);}
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){down=System.currentTimeMillis();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(System.currentTimeMillis()-down>550&&Math.abs(e.getY()-y)<dp(20))options(idx);else open(item.optString("url"));return true;}return true;}
 }
 void open(String url){try{String u=url.matches("(?i)^https?://.*")?url:"https://"+url;startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u)));}catch(Exception e){Toast.makeText(this,"사이트를 열 수 없어요.",Toast.LENGTH_SHORT).show();}}
 void options(int index){JSONObject o=sites.optJSONObject(index);if(o==null)return;String[] choices={o.optBoolean("fav")?"즐겨찾기 해제":"★ 즐겨찾기","책 정보 수정","책장에서 삭제"};new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(choices,(d,which)->{if(which==0){try{o.put("fav",!o.optBoolean("fav"));save();populate();}catch(Exception e){}}else if(which==1)edit(index);else new AlertDialog.Builder(this).setMessage("이 사이트를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(a,b)->{sites.remove(index);save();populate();}).show();}).setNegativeButton("닫기",null).show();}
 void edit(int index){JSONObject old=index<0?new JSONObject():sites.optJSONObject(index);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(20),dp(8),dp(20),0);fields.setOrientation(1);
 EditText name=new EditText(this);name.setHint("책 제목 / 사이트 이름");name.setSingleLine();name.setText(old.optString("name"));fields.addView(name);
 EditText url=new EditText(this);url.setHint("사이트 주소 (example.com)");url.setSingleLine();url.setInputType(17);url.setText(old.optString("url"));fields.addView(url);
 EditText cat=new EditText(this);cat.setHint("책장 이름 (예: 공부, AI, 게임)");cat.setSingleLine();cat.setText(old.optString("category"));fields.addView(cat);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"책장에 사이트 추가":"책 정보 수정").setView(fields).setNegativeButton("취소",null).setPositiveButton("저장",null).create();
 dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String n=name.getText().toString().trim(),u=url.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(this,"제목과 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}if(!u.matches("(?i)^https?://.*"))u="https://"+u;try{JSONObject item=new JSONObject();item.put("name",n);item.put("url",u);item.put("category",cat.getText().toString().trim());item.put("fav",old.optBoolean("fav"));if(index<0)sites.put(item);else sites.put(index,item);save();populate();dialog.dismiss();}catch(Exception e){Toast.makeText(this,"저장하지 못했어요.",Toast.LENGTH_SHORT).show();}}));dialog.show();
 }
}
