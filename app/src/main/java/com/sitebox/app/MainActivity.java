package com.sitebox.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.graphics.BitmapFactory;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
 JSONArray sites=new JSONArray();
 LinearLayout shelves;
 EditText search;
 FrameLayout stage;
 ImageView backdrop;
 boolean backgroundOnly=false;
 String filter=""; int page=0;
 // Theme palette is centralized for future theme switching.
 static class ShelfTheme {
  final int background,header,frame,innerDark,innerLight,trim,edge,shadow,gold,paper;
  ShelfTheme(int a,int b,int c,int d,int e,int f,int g,int h,int i,int j){background=a;header=b;frame=c;innerDark=d;innerLight=e;trim=f;edge=g;shadow=h;gold=i;paper=j;}
 }
 final ShelfTheme walnut=new ShelfTheme(0xFF211610,0xFF302018,0xFF9D7451,0xFF493323,0xFF795638,0xFFD1A36A,0xFF765039,0xFF2C1A12,0xFFD9B777,0xFFF3E4C9);
 ShelfTheme theme=walnut;
 final String[] categories={"공부","개발","AI","게임","영상","쇼핑","커뮤니티","생활","기타"};
 int categoryIndex(String v){for(int i=0;i<categories.length;i++)if(categories[i].equalsIgnoreCase(v.trim()))return i;return 8;}
 final int[] covers={0xFF315B57,0xFF8C493E,0xFF334C70,0xFF755A84,0xFFAD783A,0xFF566B42,0xFF5F4149,0xFF476B78};
 int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 void load(){try{sites=new JSONArray(getPreferences(0).getString("sites","[]"));}catch(Exception e){sites=new JSONArray();}}
 void save(){getPreferences(0).edit().putString("sites",sites.toString()).apply();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(47,29,21));getWindow().setNavigationBarColor(Color.rgb(47,29,21));load();draw();}
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 void draw(){
 stage=new FrameLayout(this);
 stage.setBackgroundColor(theme.background);
 backdrop=new ImageView(this);
 // Show the whole scene instead of zooming and cropping it.
 backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
 try{
  java.io.InputStream scene=getAssets().open("school_morning.jpg");
  Bitmap bmp=BitmapFactory.decodeStream(scene);scene.close();
  if(bmp!=null)backdrop.setImageBitmap(bmp);
 }catch(Exception ignored){}
 stage.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.TRANSPARENT);
 LinearLayout header=new LinearLayout(this);header.setOrientation(1);header.setPadding(dp(20),dp(45),dp(20),dp(18));
 header.setBackgroundColor(0x33000000);
 TextView eyebrow=label("맑은 아침의 학교 서재",12,0xFFFFE8BD);eyebrow.setLetterSpacing(.19f);header.addView(eyebrow);
 LinearLayout headline=new LinearLayout(this);headline.setGravity(Gravity.CENTER_VERTICAL);
 TextView title=label("SiteBox  ·  서재",27,0xFFFFFFFF);title.setTypeface(Typeface.SERIF,Typeface.BOLD);headline.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 TextView add=label("＋",30,0xFFF8EBD5);add.setGravity(Gravity.CENTER);add.setBackground(bg(0xFF775135,14));headline.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));add.setOnClickListener(v->edit(-1));
 header.addView(headline);
 search=new EditText(this);search.setSingleLine(true);search.setTextColor(0xFFF5E8D1);search.setHintTextColor(0xFFBEA994);search.setTextSize(15);search.setHint("⌕  책 제목 · 주소 · 카테고리 검색");search.setPadding(dp(14),0,dp(12),0);search.setBackground(bg(0xDD4D382B,12));search.setText(filter);
 LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(46));sp.topMargin=dp(12);header.addView(search,sp);root.addView(header);
 ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Color.TRANSPARENT);
 shelves=new LinearLayout(this);shelves.setOrientation(1);shelves.setPadding(dp(18),dp(28),dp(18),dp(25));shelves.setBackgroundColor(Color.TRANSPARENT);
 sc.addView(shelves);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
 stage.addView(root,new FrameLayout.LayoutParams(-1,-1));
 View overlay=new View(this);overlay.setBackgroundColor(Color.TRANSPARENT);overlay.setVisibility(View.GONE);
 stage.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
 overlay.setOnClickListener(v->{backgroundOnly=false;root.setVisibility(View.VISIBLE);overlay.setVisibility(View.GONE);});
 // Hold an empty part of the background to hide the bookshelf and controls.
 View.OnLongClickListener reveal=v->{backgroundOnly=true;root.setVisibility(View.INVISIBLE);overlay.setVisibility(View.VISIBLE);return true;};
 header.setOnLongClickListener(reveal);
 shelves.setOnLongClickListener(reveal);
 sc.setOnLongClickListener(reveal);
 stage.setOnLongClickListener(reveal);
 setContentView(stage);
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
 TextView count=label("나무 책장  "+(page+1)+" / "+pages+"   ·   "+visibleTotal+"권",14,0xFFE9D2B0);count.setGravity(Gravity.CENTER);nav.addView(count,new LinearLayout.LayoutParams(0,dp(42),1));
 TextView next=label("▶",19,0xFFF3D7B0);next.setGravity(Gravity.CENTER);nav.addView(next,new LinearLayout.LayoutParams(dp(45),dp(42)));next.setOnClickListener(v->{if(page<pages-1){page++;populate();}});
 nav.setBackground(bg(0xBB382619,10));
 shelves.addView(nav);
 View crown=new View(this);GradientDrawable cap=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFDDB17A,0xFF9D6940,0xFF71462B});cap.setCornerRadius(dp(4));crown.setBackground(cap);shelves.addView(crown,new LinearLayout.LayoutParams(-1,dp(13)));
 for(int shelfRow=0;shelfRow<3;shelfRow++){
 LinearLayout compartments=new LinearLayout(this);compartments.setOrientation(LinearLayout.HORIZONTAL);compartments.setPadding(dp(4),0,dp(4),0);compartments.setBackgroundColor(theme.frame);
 for(int shelfCol=0;shelfCol<3;shelfCol++){
 int cell=shelfRow*3+shelfCol;
 LinearLayout compartment=new LinearLayout(this);compartment.setOrientation(LinearLayout.VERTICAL);
 GradientDrawable back=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{theme.innerDark,theme.innerLight,theme.innerDark});back.setStroke(dp(3),theme.frame);compartment.setBackground(back);
 LinearLayout books=new LinearLayout(this);books.setGravity(Gravity.BOTTOM);books.setPadding(dp(2),dp(7),dp(2),0);
 for(int k=0;k<7;k++){
 int pos=page*7+k;
 LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(105),1);bp.setMargins(0,0,0,0);
 if(pos<grouped.get(cell).size()){int idx=grouped.get(cell).get(pos);books.addView(new BookView(this,sites.optJSONObject(idx),idx),bp);}
 else books.addView(new View(this),bp);
 }
 compartment.addView(books);
 TextView plate=label(categories[cell],11,0xFF4E3018);plate.setGravity(Gravity.CENTER);GradientDrawable brass=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFFFE5A4,0xFFD6A55B,0xFFAE773B});brass.setCornerRadius(dp(5));brass.setStroke(dp(1),0xFF8D602D);plate.setBackground(brass);LinearLayout.LayoutParams plateParams=new LinearLayout.LayoutParams(-1,dp(23));plateParams.setMargins(dp(12),dp(1),dp(12),dp(6));compartment.addView(plate,plateParams);
 final int selectedCell=cell;plate.setOnClickListener(v->edit(-1,categories[selectedCell]));
 LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(2),dp(2),dp(2),0);compartments.addView(compartment,cp);
 }
 shelves.addView(compartments);
 View lip=new View(this);GradientDrawable wood=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{theme.trim,theme.frame,theme.edge});wood.setStroke(dp(1),theme.gold);lip.setBackground(wood);shelves.addView(lip,new LinearLayout.LayoutParams(-1,dp(16)));
 View shadow=new View(this);shadow.setBackgroundColor(theme.shadow);shelves.addView(shadow,new LinearLayout.LayoutParams(-1,dp(7)));
 }
 View pedestal=new View(this);GradientDrawable plinth=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFC48C56,0xFF865331,0xFF56331E});pedestal.setBackground(plinth);shelves.addView(pedestal,new LinearLayout.LayoutParams(-1,dp(24)));
 View foot=new View(this);GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xAA2C1A12,0x002C1A12});foot.setBackground(base);shelves.addView(foot,new LinearLayout.LayoutParams(-1,dp(26)));
 TextView tip=label("책을 누르면 책 펼치기  ·  길게 누르면 관리",12,0xFFFFFFFF);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(12),0,dp(8));tip.setBackground(bg(0x99382418,8));shelves.addView(tip);
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
 p.setColor(theme.gold);canvas.drawRect(left+dp(4),top+dp(9),right-dp(6),top+dp(11),p);canvas.drawRect(left+dp(4),bottom-dp(10),right-dp(6),bottom-dp(8),p);p.setColor(0x44F9E6C6);canvas.drawRect(left+dp(2),top+dp(1),left+dp(3),bottom-dp(1),p);
 canvas.save();canvas.rotate(-90,(left+right)/2,(top+bottom)/2);
 p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(10));p.setColor(0xFFF4E3C4);
 String name=item.optString("name","Site");if(name.length()>14)name=name.substring(0,13)+"…";
 canvas.drawText(name,(left+right)/2,(top+bottom)/2+dp(3),p);canvas.restore();
 if(item.optBoolean("fav")){p.setTextSize(dp(11));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFFD477);canvas.drawText("★",w/2,top+dp(24),p);}
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){down=System.currentTimeMillis();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(System.currentTimeMillis()-down>550&&Math.abs(e.getY()-y)<dp(20))options(idx);else {animate().scaleX(0.12f).setDuration(170).withEndAction(()->{animate().scaleX(1f).setDuration(220).start();showBook(idx);}).start();}return true;}return true;}
 }
 void showBook(int index){
 JSONObject o=sites.optJSONObject(index);if(o==null)return;
 final String url=o.optString("url");
 final String safeUrl=url.matches("(?i)^https?://.*")?url:"https://"+url;
 LinearLayout spread=new LinearLayout(this);spread.setOrientation(1);spread.setPadding(dp(12),dp(12),dp(12),dp(10));spread.setBackgroundColor(theme.paper);
 TextView title=label("📖  "+o.optString("name"),21,0xFF4A2C1C);title.setTypeface(Typeface.SERIF,Typeface.BOLD);spread.addView(title);
 TextView address=label(safeUrl,12,0xFF73513B);address.setPadding(0,dp(8),0,dp(10));address.setOnClickListener(v->edit(index));spread.addView(address);
 TextView previewTitle=label("사이트 미리보기",14,0xFF6D442A);spread.addView(previewTitle);
 TextView status=label("미리보기를 불러오는 중…",12,0xFF876B52);status.setPadding(0,dp(5),0,dp(6));spread.addView(status);
 WebView web=new WebView(this);web.setBackgroundColor(0xFFFFFFFF);
 web.getSettings().setJavaScriptEnabled(true);
 web.getSettings().setDomStorageEnabled(true);
 web.getSettings().setLoadsImagesAutomatically(true);
 web.getSettings().setUseWideViewPort(true);
 web.getSettings().setLoadWithOverviewMode(true);
 web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebViewClient(new WebViewClient(){
 @Override public void onPageStarted(WebView view,String u,android.graphics.Bitmap icon){status.setText("미리보기를 불러오는 중…");}
 @Override public void onPageFinished(WebView view,String u){if(status.getText().toString().contains("불러오는 중"))status.setText("미리보기 로딩 완료 · 화면이 비어 있다면 사이트에서 앱 내 표시를 제한할 수 있어요.");}
 @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())status.setText("미리보기를 표시할 수 없어요: "+error.getDescription()+" · 아래 사이트 방문 버튼을 이용하세요.");}
 @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return !req.getUrl().getScheme().equals("https")&&!req.getUrl().getScheme().equals("http");}
 });
 spread.addView(web,new LinearLayout.LayoutParams(-1,dp(260)));
 TextView memoTitle=label("나의 세부사항 · 탭해서 수정",15,0xFF4B3021);memoTitle.setPadding(0,dp(13),0,dp(6));spread.addView(memoTitle);
 TextView memo=label(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"),14,0xFF533D2E);
 memo.setPadding(dp(12),dp(12),dp(12),dp(12));memo.setBackground(bg(0xFFE5D0AA,8));spread.addView(memo);
 memo.setOnClickListener(v->{EditText editor=new EditText(this);editor.setMinLines(4);editor.setGravity(Gravity.TOP);editor.setText(o.optString("notes"));editor.setHint("이 사이트에 관한 나만의 기록, 사용법, 아이디어...");new AlertDialog.Builder(this).setTitle("나의 세부사항 수정").setView(editor).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{try{o.put("notes",editor.getText().toString());save();memo.setText(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"));}catch(Exception e){}}).show();});
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(spread);
 AlertDialog dialog=new AlertDialog.Builder(this).setView(scroll).setNegativeButton("닫기",(d,w)->{}).setNeutralButton("정보 수정",(d,w)->edit(index)).setPositiveButton("사이트 방문",(d,w)->open(safeUrl)).create();
 dialog.setOnDismissListener(d->{web.stopLoading();web.destroy();});
 dialog.show();dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(22),-2);
 web.loadUrl(safeUrl);
 }
 void open(String url){try{String u=url.matches("(?i)^https?://.*")?url:"https://"+url;startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u)));}catch(Exception e){Toast.makeText(this,"사이트를 열 수 없어요.",Toast.LENGTH_SHORT).show();}}
 void options(int index){JSONObject o=sites.optJSONObject(index);if(o==null)return;String[] choices={o.optBoolean("fav")?"즐겨찾기 해제":"★ 즐겨찾기","책 정보 수정","책장에서 삭제"};new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(choices,(d,which)->{if(which==0){try{o.put("fav",!o.optBoolean("fav"));save();populate();}catch(Exception e){}}else if(which==1)edit(index);else new AlertDialog.Builder(this).setMessage("이 사이트를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(a,b)->{sites.remove(index);save();populate();}).show();}).setNegativeButton("닫기",null).show();}
 void edit(int index){edit(index,null);}
 void edit(int index,String selectedCategory){JSONObject old=index<0?new JSONObject():sites.optJSONObject(index);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(20),dp(8),dp(20),0);fields.setOrientation(1);
 EditText name=new EditText(this);name.setHint("책 제목 / 사이트 이름");name.setSingleLine();name.setText(old.optString("name"));fields.addView(name);
 EditText url=new EditText(this);url.setHint("사이트 주소 (example.com)");url.setSingleLine();url.setInputType(17);url.setText(old.optString("url"));fields.addView(url);
 Spinner cat=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories);cat.setAdapter(adapter);cat.setSelection(categoryIndex(selectedCategory!=null?selectedCategory:old.optString("category")));fields.addView(cat);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"책장에 사이트 추가":"책 정보 수정").setView(fields).setNegativeButton("취소",null).setPositiveButton("저장",null).create();
 dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String n=name.getText().toString().trim(),u=url.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(this,"제목과 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}if(!u.matches("(?i)^https?://.*"))u="https://"+u;try{JSONObject item=new JSONObject();item.put("name",n);item.put("url",u);item.put("category",categories[cat.getSelectedItemPosition()]);item.put("fav",old.optBoolean("fav"));item.put("notes",old.optString("notes"));if(index<0)sites.put(item);else sites.put(index,item);save();populate();dialog.dismiss();}catch(Exception e){Toast.makeText(this,"저장하지 못했어요.",Toast.LENGTH_SHORT).show();}}));dialog.show();
 }
} ArrayList<ArrayList<Integer>> grouped=new ArrayList<>();
 for(int c=0;c<9;c++)grouped.add(new ArrayList<Integer>());
 int total=0;
 for(int i=0;i<sites.length();i++){
 JSONObject o=sites.optJSONObject(i);if(o==null)continue;
 String all=o.optString("name")+" "+o.optString("url")+" "+o.optString("category");
 if(all.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))){
 grouped.get(categoryIndex(o.optString("category"))).add(i);total++;
 }
 }
 for(ArrayList<Integer> group:grouped)Collections.sort(group,(a,b)->Boolean.compare(sites.optJSONObject(b).optBoolean("fav"),sites.optJSONObject(a).optBoolean("fav")));
 int pages=1;for(ArrayList<Integer> group:grouped)pages=Math.max(pages,(group.size()+6)/7);
 page=Math.max(0,Math.min(page,pages-1));
 final int visibleTotal=total;
 LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
 TextView prev=label("◀",19,0xFFF3D7B0);prev.setGravity(Gravity.CENTER);nav.addView(prev,new LinearLayout.LayoutParams(dp(45),dp(42)));prev.setOnClickListener(v->{if(page>0){page--;populate();}});
 TextView count=label("나무 책장  "+(page+1)+" / "+pages+"   ·   "+ids.size()+"권",14,0xFFE9D2B0);count.setGravity(Gravity.CENTER);nav.addView(count,new LinearLayout.LayoutParams(0,dp(42),1));
 TextView next=label("▶",19,0xFFF3D7B0);next.setGravity(Gravity.CENTER);nav.addView(next,new LinearLayout.LayoutParams(dp(45),dp(42)));next.setOnClickListener(v->{if(page<pages-1){page++;populate();}});
 nav.setBackground(bg(0xBB382619,10));
 shelves.addView(nav);
 View crown=new View(this);GradientDrawable cap=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFDDB17A,0xFF9D6940,0xFF71462B});cap.setCornerRadius(dp(4));crown.setBackground(cap);shelves.addView(crown,new LinearLayout.LayoutParams(-1,dp(13)));
 for(int shelfRow=0;shelfRow<3;shelfRow++){
 LinearLayout compartments=new LinearLayout(this);compartments.setOrientation(LinearLayout.HORIZONTAL);compartments.setPadding(dp(4),0,dp(4),0);compartments.setBackgroundColor(theme.frame);
 for(int shelfCol=0;shelfCol<3;shelfCol++){
 int cell=shelfRow*3+shelfCol;
 LinearLayout compartment=new LinearLayout(this);compartment.setOrientation(LinearLayout.VERTICAL);
 GradientDrawable back=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{theme.innerDark,theme.innerLight,theme.innerDark});back.setStroke(dp(3),theme.frame);compartment.setBackground(back);
 LinearLayout books=new LinearLayout(this);books.setGravity(Gravity.BOTTOM);books.setPadding(dp(2),dp(7),dp(2),0);
 for(int k=0;k<7;k++){
 int pos=page*63+cell*7+k;
 LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(105),1);bp.setMargins(0,0,0,0);
 if(pos<ids.size()){int idx=ids.get(pos);books.addView(new BookView(this,sites.optJSONObject(idx),idx),bp);}
 else books.addView(new View(this),bp);
 }
 compartment.addView(books);
 LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(2),dp(2),dp(2),0);compartments.addView(compartment,cp);
 }
 shelves.addView(compartments);
 View lip=new View(this);GradientDrawable wood=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{theme.trim,theme.frame,theme.edge});wood.setStroke(dp(1),theme.gold);lip.setBackground(wood);shelves.addView(lip,new LinearLayout.LayoutParams(-1,dp(16)));
 View shadow=new View(this);shadow.setBackgroundColor(theme.shadow);shelves.addView(shadow,new LinearLayout.LayoutParams(-1,dp(7)));
 }
 View pedestal=new View(this);GradientDrawable plinth=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFC48C56,0xFF865331,0xFF56331E});pedestal.setBackground(plinth);shelves.addView(pedestal,new LinearLayout.LayoutParams(-1,dp(24)));
 View foot=new View(this);GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xAA2C1A12,0x002C1A12});foot.setBackground(base);shelves.addView(foot,new LinearLayout.LayoutParams(-1,dp(26)));
 TextView tip=label("책을 누르면 책 펼치기  ·  길게 누르면 관리",12,0xFFFFFFFF);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(12),0,dp(8));tip.setBackground(bg(0x99382418,8));shelves.addView(tip);
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
 p.setColor(theme.gold);canvas.drawRect(left+dp(4),top+dp(9),right-dp(6),top+dp(11),p);canvas.drawRect(left+dp(4),bottom-dp(10),right-dp(6),bottom-dp(8),p);p.setColor(0x44F9E6C6);canvas.drawRect(left+dp(2),top+dp(1),left+dp(3),bottom-dp(1),p);
 canvas.save();canvas.rotate(-90,(left+right)/2,(top+bottom)/2);
 p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(10));p.setColor(0xFFF4E3C4);
 String name=item.optString("name","Site");if(name.length()>14)name=name.substring(0,13)+"…";
 canvas.drawText(name,(left+right)/2,(top+bottom)/2+dp(3),p);canvas.restore();
 if(item.optBoolean("fav")){p.setTextSize(dp(11));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFFD477);canvas.drawText("★",w/2,top+dp(24),p);}
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){down=System.currentTimeMillis();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(System.currentTimeMillis()-down>550&&Math.abs(e.getY()-y)<dp(20))options(idx);else {animate().scaleX(0.12f).setDuration(170).withEndAction(()->{animate().scaleX(1f).setDuration(220).start();showBook(idx);}).start();}return true;}return true;}
 }
 void showBook(int index){
 JSONObject o=sites.optJSONObject(index);if(o==null)return;
 final String url=o.optString("url");
 final String safeUrl=url.matches("(?i)^https?://.*")?url:"https://"+url;
 LinearLayout spread=new LinearLayout(this);spread.setOrientation(1);spread.setPadding(dp(12),dp(12),dp(12),dp(10));spread.setBackgroundColor(theme.paper);
 TextView title=label("📖  "+o.optString("name"),21,0xFF4A2C1C);title.setTypeface(Typeface.SERIF,Typeface.BOLD);spread.addView(title);
 TextView address=label(safeUrl,12,0xFF73513B);address.setPadding(0,dp(8),0,dp(10));address.setOnClickListener(v->edit(index));spread.addView(address);
 TextView previewTitle=label("사이트 미리보기",14,0xFF6D442A);spread.addView(previewTitle);
 TextView status=label("미리보기를 불러오는 중…",12,0xFF876B52);status.setPadding(0,dp(5),0,dp(6));spread.addView(status);
 WebView web=new WebView(this);web.setBackgroundColor(0xFFFFFFFF);
 web.getSettings().setJavaScriptEnabled(true);
 web.getSettings().setDomStorageEnabled(true);
 web.getSettings().setLoadsImagesAutomatically(true);
 web.getSettings().setUseWideViewPort(true);
 web.getSettings().setLoadWithOverviewMode(true);
 web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebViewClient(new WebViewClient(){
 @Override public void onPageStarted(WebView view,String u,android.graphics.Bitmap icon){status.setText("미리보기를 불러오는 중…");}
 @Override public void onPageFinished(WebView view,String u){if(status.getText().toString().contains("불러오는 중"))status.setText("미리보기 로딩 완료 · 화면이 비어 있다면 사이트에서 앱 내 표시를 제한할 수 있어요.");}
 @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())status.setText("미리보기를 표시할 수 없어요: "+error.getDescription()+" · 아래 사이트 방문 버튼을 이용하세요.");}
 @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return !req.getUrl().getScheme().equals("https")&&!req.getUrl().getScheme().equals("http");}
 });
 spread.addView(web,new LinearLayout.LayoutParams(-1,dp(260)));
 TextView memoTitle=label("나의 세부사항 · 탭해서 수정",15,0xFF4B3021);memoTitle.setPadding(0,dp(13),0,dp(6));spread.addView(memoTitle);
 TextView memo=label(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"),14,0xFF533D2E);
 memo.setPadding(dp(12),dp(12),dp(12),dp(12));memo.setBackground(bg(0xFFE5D0AA,8));spread.addView(memo);
 memo.setOnClickListener(v->{EditText editor=new EditText(this);editor.setMinLines(4);editor.setGravity(Gravity.TOP);editor.setText(o.optString("notes"));editor.setHint("이 사이트에 관한 나만의 기록, 사용법, 아이디어...");new AlertDialog.Builder(this).setTitle("나의 세부사항 수정").setView(editor).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{try{o.put("notes",editor.getText().toString());save();memo.setText(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"));}catch(Exception e){}}).show();});
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(spread);
 AlertDialog dialog=new AlertDialog.Builder(this).setView(scroll).setNegativeButton("닫기",(d,w)->{}).setNeutralButton("정보 수정",(d,w)->edit(index)).setPositiveButton("사이트 방문",(d,w)->open(safeUrl)).create();
 dialog.setOnDismissListener(d->{web.stopLoading();web.destroy();});
 dialog.show();dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(22),-2);
 web.loadUrl(safeUrl);
 }
 void open(String url){try{String u=url.matches("(?i)^https?://.*")?url:"https://"+url;startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u)));}catch(Exception e){Toast.makeText(this,"사이트를 열 수 없어요.",Toast.LENGTH_SHORT).show();}}
 void options(int index){JSONObject o=sites.optJSONObject(index);if(o==null)return;String[] choices={o.optBoolean("fav")?"즐겨찾기 해제":"★ 즐겨찾기","책 정보 수정","책장에서 삭제"};new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(choices,(d,which)->{if(which==0){try{o.put("fav",!o.optBoolean("fav"));save();populate();}catch(Exception e){}}else if(which==1)edit(index);else new AlertDialog.Builder(this).setMessage("이 사이트를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(a,b)->{sites.remove(index);save();populate();}).show();}).setNegativeButton("닫기",null).show();}
 void edit(int index){JSONObject old=index<0?new JSONObject():sites.optJSONObject(index);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(20),dp(8),dp(20),0);fields.setOrientation(1);
 EditText name=new EditText(this);name.setHint("책 제목 / 사이트 이름");name.setSingleLine();name.setText(old.optString("name"));fields.addView(name);
 EditText url=new EditText(this);url.setHint("사이트 주소 (example.com)");url.setSingleLine();url.setInputType(17);url.setText(old.optString("url"));fields.addView(url);
 EditText cat=new EditText(this);cat.setHint("책장 이름 (예: 공부, AI, 게임)");cat.setSingleLine();cat.setText(old.optString("category"));fields.addView(cat);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"책장에 사이트 추가":"책 정보 수정").setView(fields).setNegativeButton("취소",null).setPositiveButton("저장",null).create();
 dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String n=name.getText().toString().trim(),u=url.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(this,"제목과 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}if(!u.matches("(?i)^https?://.*"))u="https://"+u;try{JSONObject item=new JSONObject();item.put("name",n);item.put("url",u);item.put("category",cat.getText().toString().trim());item.put("fav",old.optBoolean("fav"));item.put("notes",old.optString("notes"));if(index<0)sites.put(item);else sites.put(index,item);save();populate();dialog.dismiss();}catch(Exception e){Toast.makeText(this,"저장하지 못했어요.",Toast.LENGTH_SHORT).show();}}));dialog.show();
 }
}
