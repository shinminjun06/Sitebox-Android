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
 TextView searchToggle;
 LinearLayout header,headline,pagingBar;
 boolean searchExpanded=false;
 FrameLayout stage;
 RoomPlateView backdrop,roomForeground,outsideLayer;
 RoomExperience roomExperience;
 boolean backgroundOnly=false;
 String filter=""; int page=0;
 static final int BOOKS_PER_SHELF=BookLayout.CAPACITY;
 ThemeAssets assets;
 BookView dragSource;
 ScrollView shelfScroll;
 long lastDragScroll=0;
 TextView pageSummary;
 LinearLayout controls;
 View revealOverlay,lampSwitch;
 boolean studyLampOn=true;
 BookshelfView currentCabinet;
 ProjectedCabinetView miniature;
 FurnitureShadow sceneShadow;
 Bitmap sceneBackdrop;
 FrameLayout roomLayer;
 StudyLiveView studyLive;
 WeatherRepository weather;
 boolean foreground,motionEnabled=true;
 WeatherState ambientPreview;long ambientPreviewTime;
 TextView ambientStatus;
 final Handler ambientHandler=new Handler(Looper.getMainLooper());
 final Runnable ambientTick=new Runnable(){public void run(){if(!foreground||!backgroundThemeId.equals("study"))return;updateAmbient();weather.refresh(false);ambientHandler.postDelayed(this,60000);}};
 String backgroundThemeId="study";
 SceneTheme renderedSceneTheme=SceneTheme.ALL[0];
 boolean sceneCollapsed=false,sceneAnimating=false,restoreCollapsed=false;


 int shelfThemeIndex=0,bookThemeIndex=0;
 final String[] shelfThemeNames={"햇살 원목","짙은 월넛","화이트 오크"};
 final String[] bookThemeNames={"고전 양장본","파스텔 북","모던 북"};
 // Theme palette is centralized for future theme switching.
 static class ShelfTheme {
  final int background,header,frame,innerDark,innerLight,trim,edge,shadow,gold,paper;
  ShelfTheme(int a,int b,int c,int d,int e,int f,int g,int h,int i,int j){background=a;header=b;frame=c;innerDark=d;innerLight=e;trim=f;edge=g;shadow=h;gold=i;paper=j;}
 }
 final ShelfTheme walnut=new ShelfTheme(0xFF211610,0xFF302018,0xFFB97E4C,0xFF674029,0xFF986038,0xFFE6B77D,0xFF986038,0xFF3C2518,0xFFECC681,0xFFF3E4C9);
 ShelfTheme theme=walnut;
 String[] categories={"공부","개발","AI","게임","영상","쇼핑","커뮤니티","생활","기타"};
 int categoryIndex(String v){for(int i=0;i<categories.length;i++)if(categories[i].equalsIgnoreCase(v.trim()))return i;return 8;}
 final int[] covers={0xFF152B49,0xFF74262D,0xFFE9D8B6,0xFF133D38,0xFF693525,0xFF234C4B,0xFF253858,0xFFBBA17D};
 int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 BackupController backups;
 void load(){android.content.SharedPreferences prefs=getPreferences(0);studyLampOn=prefs.getBoolean("studyLampOn",true);boolean introduced=prefs.getBoolean("studyThemeIntroduced",false);backgroundThemeId=SceneTheme.initialTheme(prefs.getString(SceneTheme.PREFERENCE_KEY,null),introduced);if(!introduced||!backgroundThemeId.equals(prefs.getString(SceneTheme.PREFERENCE_KEY,null)))prefs.edit().putBoolean("studyThemeIntroduced",true).putString(SceneTheme.PREFERENCE_KEY,backgroundThemeId).apply();shelfThemeIndex=Math.max(0,Math.min(2,getPreferences(0).getInt("shelfTheme",0)));bookThemeIndex=Math.max(0,Math.min(2,getPreferences(0).getInt("bookTheme",0)));try{sites=new JSONArray(getPreferences(0).getString("sites","[]"));}catch(Exception e){sites=new JSONArray();}try{JSONArray saved=new JSONArray(getPreferences(0).getString("categories","[]"));if(saved.length()==9)for(int i=0;i<9;i++)categories[i]=saved.optString(i,categories[i]);}catch(Exception ignored){}}
 void save(){JSONArray names=new JSONArray();for(String c:categories)names.put(c);getPreferences(0).edit().putString("sites",sites.toString()).putString("categories",names.toString()).putInt("shelfTheme",shelfThemeIndex).putInt("bookTheme",bookThemeIndex).apply();if(backups!=null)backups.localChanged();}
 @Override public void onCreate(Bundle b){
 super.onCreate(b);assets=new ThemeAssets(this);load();motionEnabled=getPreferences(0).getBoolean("studyMotion",true);weather=new WeatherRepository(this,(state,status)->updateAmbient());
 restoreCollapsed=b==null;
 if(b!=null){filter=b.getString("filter","");page=b.getInt("page",0);restoreCollapsed=b.getBoolean("sceneCollapsed",false);searchExpanded=b.getBoolean("searchExpanded",!filter.isEmpty());}
 getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
 if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
 else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
 getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0x660F211C);
 draw();backups=new BackupController(this);if(b!=null)backups.restoreState(b);
 if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->onBackPressed());
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(backups!=null)backups.saveState(out);out.putString("filter",filter);out.putBoolean("searchExpanded",searchExpanded);out.putInt("page",page);out.putBoolean("sceneCollapsed",sceneCollapsed);}
 @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=TRIM_MEMORY_RUNNING_LOW)assets.clear();}

 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 void draw(){
 stage=new LibraryStage(this,()->collapseToScene(true));stage.setBackgroundColor(0xff20231f);stage.setClipChildren(false);
 backdrop=new RoomPlateView(this,false);
 // Measure the live layer on the first pass; backdrop layout may enable it mid-layout.
 roomLayer=new FrameLayout(this);outsideLayer=new RoomPlateView(this,true);roomLayer.addView(outsideLayer,new FrameLayout.LayoutParams(-1,-1));studyLive=new StudyLiveView(this);studyLive.setVisibility(View.INVISIBLE);roomLayer.addView(studyLive,new FrameLayout.LayoutParams(-1,-1));roomLayer.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));stage.addView(roomLayer,new FrameLayout.LayoutParams(-1,-1));
 backdrop.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)updateSceneBackdrop();});
 LinearLayout root=new LinearLayout(this);controls=root;root.setOrientation(LinearLayout.VERTICAL);root.setFocusableInTouchMode(true);root.setBackgroundColor(0x24FFF6DF);
 root.setOnApplyWindowInsetsListener((view,insets)->{
  if(Build.VERSION.SDK_INT>=30){android.graphics.Insets sys=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());view.setPadding(sys.left,sys.top,sys.right,sys.bottom);}
  else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
  return insets;
 });
 header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);header.setPadding(dp(12),dp(4),dp(12),dp(4));header.setBackground(bg(0xDA173B32,0));
 headline=new LinearLayout(this);headline.setGravity(Gravity.CENTER_VERTICAL);
 LinearLayout branding=new LinearLayout(this);branding.setOrientation(LinearLayout.VERTICAL);
 TextView title=label("SiteBox",24,0xFFFFF4DD);title.setTypeface(Typeface.SERIF,Typeface.BOLD);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);branding.addView(title);
 headline.addView(branding,new LinearLayout.LayoutParams(0,-2,1));
 TextView settings=label("설정",13,0xFFFFF1D5);settings.setGravity(Gravity.CENTER);settings.setContentDescription("카테고리와 테마 설정");headline.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
 settings.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("나의 서재").setItems(new String[]{"배경 테마","책 스타일","카테고리 이름 수정","시간 · 날씨","Google 계정 · 자동 동기화","앱 정보"},(d,w)->{if(w==0)showBackgroundThemes();else if(w==1)showThemes();else if(w==2)manageCategories();else if(w==3)showAmbientSettings();else if(w==4)backups.show();else new AlertDialog.Builder(this).setTitle("SiteBox "+BuildConfig.VERSION_NAME).setMessage("소스 버전: "+BuildConfig.SOURCE_REVISION+"\n책을 길게 눌러 칸 사이로 이동하세요.\n명패를 누르면 해당 칸에 책을 추가합니다.").setPositiveButton("닫기",null).show();}).show());
 TextView add=label("＋",28,0xFF263B30);add.setGravity(Gravity.CENTER);add.setContentDescription("북마크 추가");add.setBackground(bg(0xFFE5C28A,14));headline.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));add.setOnClickListener(v->edit(-1));searchToggle=label("검색",13,0xFFFFF1D5);searchToggle.setGravity(Gravity.CENTER);searchToggle.setContentDescription("검색 펼치기");headline.addView(searchToggle,new LinearLayout.LayoutParams(dp(48),dp(48)));searchToggle.setOnClickListener(v->setSearchExpanded(!searchExpanded));header.addView(headline);
 search=new EditText(this);search.setSingleLine(true);search.setTextSize(13);search.setTextColor(0xFF243E32);search.setHintTextColor(0xFF667568);search.setHint("검색 · 제목, 주소, 카테고리");search.setPadding(dp(14),0,dp(12),0);search.setBackground(bg(0xF2FFF9EB,12));search.setText(filter);
 search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
 LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(7);header.addView(search,sp);setSearchExpanded(searchExpanded);root.addView(header);
 shelfScroll=new ShelfScrollView(this);shelfScroll.setFillViewport(true);shelfScroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->{if(roomExperience!=null)roomExperience.relayout();});shelfScroll.setClipToPadding(false);
 shelves=new LinearLayout(this);shelves.setOrientation(LinearLayout.VERTICAL);shelves.setGravity(Gravity.CENTER_HORIZONTAL);shelves.setPadding(0,dp(8),0,dp(14));
 shelfScroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)resizeShelfContents();});
 shelfScroll.addView(shelves);root.addView(shelfScroll,new LinearLayout.LayoutParams(-1,0,1));stage.addView(root,new FrameLayout.LayoutParams(-1,-1));
 revealOverlay=new View(this);revealOverlay.setContentDescription("서재 화면으로 돌아가기");revealOverlay.setVisibility(View.GONE);stage.addView(revealOverlay,new FrameLayout.LayoutParams(-1,-1));
 revealOverlay.setOnClickListener(v->{backgroundOnly=false;root.setVisibility(View.VISIBLE);revealOverlay.setVisibility(View.GONE);});
 // Empty-area long presses are inert. BookView alone owns long-press drag.
 sceneShadow=new FurnitureShadow(this);sceneShadow.setVisibility(View.GONE);stage.addView(sceneShadow,new FrameLayout.LayoutParams(1,1));
 miniature=new ProjectedCabinetView(this);miniature.setContentDescription("작아진 책장 열기");miniature.setVisibility(View.GONE);stage.addView(miniature,new FrameLayout.LayoutParams(-1,-1));miniature.setOnClickListener(v->expandFromScene(true));
 roomForeground=new RoomPlateView(this,true);stage.addView(roomForeground,new FrameLayout.LayoutParams(-1,-1));
 lampSwitch=new View(this);lampSwitch.setFocusable(true);lampSwitch.setVisibility(View.GONE);
 lampSwitch.setOnClickListener(v->{if(sceneCollapsed&&!sceneAnimating&&renderedSceneTheme.id.equals("study")){studyLampOn=!studyLampOn;getPreferences(0).edit().putBoolean("studyLampOn",studyLampOn).apply();updateLampArtwork();updateLampSwitch();}});
 stage.addView(lampSwitch,new FrameLayout.LayoutParams(1,1));
 setContentView(stage);root.requestApplyInsets();roomExperience=new RoomExperience(this);((LibraryStage)stage).setTransitionGuard(()->sceneAnimating,(x,y)->miniature.hitFrame(x,y),()->expandFromScene(true));
 search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){filter=s.toString();updateSearchIndicator();page=0;populate();}public void afterTextChanged(Editable e){}});
 populate();root.requestFocus();
 if(!getPreferences(0).getBoolean("gestureTipSeen",false)){
  Toast.makeText(this,"책장을 누르면 확대 · 두 손가락으로 축소",Toast.LENGTH_LONG).show();getPreferences(0).edit().putBoolean("gestureTipSeen",true).apply();
 }
 if(restoreCollapsed){
  controls.setVisibility(View.INVISIBLE);
  stage.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(currentCabinet.getWidth()>0){stage.getViewTreeObserver().removeOnPreDrawListener(this);collapseToScene(false);}return true;}});
 }

 }
 
 /** Changing a room touches only its own preference and the backdrop/miniature geometry. */
 void applyBackgroundTheme(String id){
  backgroundThemeId=SceneTheme.find(id).id;
  getPreferences(0).edit().putString(SceneTheme.PREFERENCE_KEY,backgroundThemeId).apply();if(backups!=null)backups.localChanged();
  updateSceneBackdrop();
 }
 void updateSceneBackdrop(){
  if(stage==null||stage.getWidth()==0||stage.getHeight()==0||roomExperience==null)return;
  renderedSceneTheme=SceneTheme.find(backgroundThemeId);roomExperience.relayout();
  studyLive.setMotionEnabled(motionEnabled);restartAmbient();updateLampSwitch();
 }
 void showBackgroundThemes(){
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(6),dp(14),0);
  TextView help=label("배경을 누르면 바로 적용됩니다. 책장과 책 테마는 그대로 유지돼요.",13,0xFF574532);help.setPadding(dp(4),dp(4),dp(4),dp(10));content.addView(help);
  ListView choices=new ListView(this);choices.setDividerHeight(dp(6));
  BaseAdapter adapter=new BaseAdapter(){
   public int getCount(){return SceneTheme.ALL.length;}
   public Object getItem(int position){return SceneTheme.ALL[position];}
   public long getItemId(int position){return position;}
   public View getView(int position,View recycled,ViewGroup parent){
    LinearLayout row;
    if(recycled instanceof LinearLayout)row=(LinearLayout)recycled;
    else {
     row=new LinearLayout(MainActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(7),dp(12),dp(7));
     ImageView preview=new ImageView(MainActivity.this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(preview,new LinearLayout.LayoutParams(dp(72),dp(94)));
     TextView name=label("",16,0xFF3D291D);name.setPadding(dp(16),0,0,0);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
     row.addView(label("",20,0xFF305B42),new LinearLayout.LayoutParams(dp(28),-2));
    }
    SceneTheme scene=SceneTheme.ALL[position];boolean selected=scene.id.equals(backgroundThemeId);
    ((ImageView)row.getChildAt(0)).setImageBitmap(assets.scenePreview(scene));
    ((TextView)row.getChildAt(1)).setText(scene.name);((TextView)row.getChildAt(2)).setText(selected?"✓":"");
    row.setBackground(bg(selected?0xFFE8D5B3:0xFFF7F0E3,10));row.setContentDescription(scene.name);row.setSelected(selected);
    return row;
   }
  };
  choices.setAdapter(adapter);choices.setOnItemClickListener((parent,view,position,id)->{applyBackgroundTheme(SceneTheme.ALL[position].id);adapter.notifyDataSetChanged();});
  content.addView(choices,new LinearLayout.LayoutParams(-1,Math.min(dp(370),(int)(getResources().getDisplayMetrics().heightPixels*.50f))));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("배경 테마").setView(content).setPositiveButton("닫기",null).create();
  dialog.setOnShowListener(d->{for(int i=0;i<SceneTheme.ALL.length;i++)if(SceneTheme.ALL[i].id.equals(backgroundThemeId))choices.setSelection(i);});dialog.show();
 }
 void showThemes(){
 LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(22),dp(14),dp(22),dp(8));
 TextView shelfLabel=label("책장 색상",16,0xFF3D291D);layout.addView(shelfLabel);
 Spinner shelfPicker=new Spinner(this);shelfPicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,shelfThemeNames));shelfPicker.setSelection(shelfThemeIndex);layout.addView(shelfPicker);
 TextView bookLabel=label("책 테마",16,0xFF3D291D);bookLabel.setPadding(0,dp(18),0,0);layout.addView(bookLabel);
 Spinner bookPicker=new Spinner(this);bookPicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,bookThemeNames));bookPicker.setSelection(bookThemeIndex);layout.addView(bookPicker);
 new AlertDialog.Builder(this).setTitle("책 스타일").setView(layout).setNegativeButton("취소",null).setPositiveButton("적용",(d,w)->{shelfThemeIndex=shelfPicker.getSelectedItemPosition();bookThemeIndex=bookPicker.getSelectedItemPosition();save();assets.clear();populate();}).show();
 }
 void setSearchExpanded(boolean expanded){
  searchExpanded=expanded;if(search==null)return;
  search.setVisibility(expanded?View.VISIBLE:View.GONE);searchToggle.setSelected(expanded);updateSearchIndicator();
  if(!expanded){search.clearFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);}
 }
 void updateSearchIndicator(){searchToggle.setText(filter.isEmpty()?"검색":"검색 중");searchToggle.setContentDescription((searchExpanded?"검색 접기":"검색 펼치기")+(filter.isEmpty()?"":" · 현재 검색: "+filter));}
 int shelfContentWidth(){int available=shelfScroll.getWidth();if(available<=0)available=getResources().getDisplayMetrics().widthPixels;int height=shelfScroll.getHeight();if(height<=0)height=getResources().getDisplayMetrics().heightPixels-dp(110);return RoomModelGeometry.editorWidth(available,height,getResources().getDisplayMetrics().density);}
 void resizeShelfContents(){int width=shelfContentWidth();for(int i=0;i<shelves.getChildCount();i++){View child=shelves.getChildAt(i);ViewGroup.LayoutParams lp=child.getLayoutParams();if(lp.width!=width){lp.width=width;child.setLayoutParams(lp);}}}
 void populate(){
 shelves.removeAllViews();
 ArrayList<ArrayList<Integer>> grouped=new ArrayList<>();for(int i=0;i<9;i++)grouped.add(new ArrayList<>());
 int visibleTotal=0;String query=filter.toLowerCase(Locale.ROOT);
 for(int i=0;i<sites.length();i++){
  JSONObject o=sites.optJSONObject(i);if(o==null)continue;
  String value=o.optString("name")+" "+o.optString("url")+" "+o.optString("category");
  if(value.toLowerCase(Locale.ROOT).contains(query)){grouped.get(categoryIndex(o.optString("category"))).add(i);visibleTotal++;}
 }
 int pages=1;for(ArrayList<Integer> group:grouped){sortBooks(group);pages=Math.max(pages,(group.size()+BOOKS_PER_SHELF-1)/BOOKS_PER_SHELF);}
 page=Math.max(0,Math.min(page,pages-1));final int totalPages=pages;
 LinearLayout nav=new LinearLayout(this);pagingBar=nav;nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackground(bg(0xD9234035,12));
 TextView prev=label("‹",26,0xFFFFEDCA);prev.setGravity(Gravity.CENTER);prev.setContentDescription("이전 페이지");prev.setEnabled(page>0);prev.setAlpha(page>0?1f:.35f);nav.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(48)));prev.setOnClickListener(v->{if(page>0){page--;populate();}});
 pageSummary=label((page+1)+" / "+pages+"  ·  "+visibleTotal+"권",12,0xFFFFEACA);pageSummary.setGravity(Gravity.CENTER);nav.addView(pageSummary,new LinearLayout.LayoutParams(0,dp(48),1));
 TextView next=label("›",26,0xFFFFEDCA);next.setGravity(Gravity.CENTER);next.setContentDescription("다음 페이지");next.setEnabled(page<pages-1);next.setAlpha(page<pages-1?1f:.35f);nav.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));next.setOnClickListener(v->{if(page<totalPages-1){page++;populate();}});
 int maxWidth=shelfContentWidth();
 nav.setVisibility(pages>1?View.VISIBLE:View.GONE);
 shelves.addView(nav,new LinearLayout.LayoutParams(maxWidth,dp(48)));
 Bitmap frame=assets.get(RoomScene.ROOT+"cabinet_front.webp");
 BookshelfView furniture=new BookshelfView(this,frame!=null?frame:assets.shelf(shelfThemeIndex));furniture.setFinish(shelfThemeIndex);furniture.setModelFrame();currentCabinet=furniture;
 for(int cell=0;cell<9;cell++){
  final int category=cell;LinearLayout books=new LinearLayout(this);books.setGravity(Gravity.BOTTOM);books.setPadding(dp(2),dp(6),dp(2),0);books.setTag(cell);
  for(int slot=0;slot<BOOKS_PER_SHELF;slot++){
   int pos=page*BOOKS_PER_SHELF+slot;int anchor=pos<grouped.get(cell).size()?grouped.get(cell).get(pos):-1;
   View book=anchor>=0?new BookView(this,sites.optJSONObject(anchor),anchor):new View(this);
   final int target=anchor;book.setOnDragListener((v,e)->handleBookDrop(v,e,category,target));
   books.addView(book,new LinearLayout.LayoutParams(0,-1,1));
  }
  books.setOnDragListener((v,e)->handleBookDrop(v,e,category,-1));furniture.addView(books,new FrameLayout.LayoutParams(-1,-1));
  TextView plate=new CategoryPlateView(this);plate.setText(categories[cell]);plate.setTextSize(10);plate.setTextColor(0xFF433019);plate.setTag(9+cell);plate.setSingleLine(true);plate.setEllipsize(TextUtils.TruncateAt.END);plate.setGravity(Gravity.CENTER);plate.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  GradientDrawable brass=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFFFFE6A9,0xFFD2A65D,0xFFF1CF8C,0xFFAB7838});brass.setCornerRadius(dp(4));brass.setStroke(dp(1),0xFF8A602C);plate.setBackground(brass);plate.setElevation(dp(2));plate.setContentDescription(categories[cell]+" 카테고리에 북마크 추가");
  plate.setOnClickListener(v->edit(-1,categories[category]));plate.setOnDragListener((v,e)->handleBookDrop(v,e,category,-1));furniture.addView(plate,new FrameLayout.LayoutParams(-1,-1));
 }
 LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(maxWidth,-2);fp.topMargin=dp(8);shelves.addView(furniture,fp);
 if(roomExperience!=null)roomExperience.bind();
 if(visibleTotal==0){TextView empty=label(filter.isEmpty()?"＋ 또는 명패를 눌러 첫 책을 꽂아보세요":"검색 결과가 없어요",12,0xFFFFF5DE);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(8),dp(8),dp(8),dp(8));empty.setBackground(bg(0xCC244036,12));shelves.addView(empty,new LinearLayout.LayoutParams(maxWidth,-2));}
 }
 void sortBooks(List<Integer> group){Collections.sort(group,(a,b)->{JSONObject x=sites.optJSONObject(a),y=sites.optJSONObject(b);int order=Long.compare(x.optLong("sortOrder",a*1000L),y.optLong("sortOrder",b*1000L));return order!=0?order:Integer.compare(a,b);});}
 class BookView extends BookArtworkView {
  final int idx;
  BookView(Context c,JSONObject obj,int index){super(c,obj,assets.book(bookThemeIndex,Math.floorMod(obj.optString("name").hashCode(),4)),bookThemeIndex);idx=index;setFocusable(true);setClickable(true);setOnClickListener(v->showBook(idx));
   setOnLongClickListener(v->{performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);boolean started=startDragAndDrop(ClipData.newPlainText("sitebox-book",String.valueOf(idx)),new View.DragShadowBuilder(this),Integer.valueOf(idx),0);if(started){dragSource=this;setAlpha(.25f);}return started;});
  }
 }
 RectF cabinetBounds(){
  int[] cabinetPosition=new int[2],stagePosition=new int[2];currentCabinet.getLocationOnScreen(cabinetPosition);stage.getLocationOnScreen(stagePosition);
  float x=cabinetPosition[0]-stagePosition[0],y=cabinetPosition[1]-stagePosition[1];return new RectF(x,y,x+currentCabinet.getWidth(),y+currentCabinet.getHeight());
 }
 float[] sceneBounds(){float[] result=new float[4];if(roomExperience!=null)roomExperience.roomBounds(result);return result;}
 void collapseToScene(boolean animate){if(roomExperience!=null)roomExperience.request(true,animate);}
 void expandFromScene(boolean animate){if(roomExperience!=null)roomExperience.request(false,animate);}
 @Override public void onBackPressed(){
  if(roomExperience!=null&&(roomExperience.transition.progress()<.9999f||sceneAnimating)){collapseToScene(true);return;}
  super.onBackPressed();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(backups!=null)backups.onActivityResult(request,result,data);}
 @Override protected void onDestroy(){if(backups!=null)backups.close();ambientHandler.removeCallbacksAndMessages(null);if(roomExperience!=null)roomExperience.dispose();if(studyLive!=null)studyLive.release();if(weather!=null)weather.close();if(controls!=null)controls.animate().cancel();super.onDestroy();}

 @Override protected void onResume(){super.onResume();foreground=true;restartAmbient();if(backups!=null)backups.resume();}
 @Override protected void onPause(){foreground=false;if(backups!=null)backups.pause();ambientHandler.removeCallbacks(ambientTick);if(studyLive!=null)studyLive.setActive(false);if(roomExperience!=null)roomExperience.model.setActive(false);super.onPause();}
 void restartAmbient(){ambientHandler.removeCallbacks(ambientTick);if(studyLive==null)return;studyLive.setActive(false);if(roomExperience!=null)roomExperience.model.setActive(foreground);if(foreground&&backgroundThemeId.equals("study")){updateAmbient();weather.refresh(false);ambientHandler.postDelayed(ambientTick,60000);}}
 void updateLampSwitch(){
  if(studyLive==null||lampSwitch==null)return;
  studyLive.setLampEnabled(studyLampOn);sceneShadow.setLampEnabled(studyLampOn);miniature.setLampEnabled(studyLampOn);
  lampSwitch.setContentDescription(studyLampOn?"조명 끄기":"조명 켜기");
  if(!sceneCollapsed||sceneAnimating||roomExperience==null){lampSwitch.setVisibility(View.GONE);return;}
  float[] region=new float[4];roomExperience.lampBounds(region);
  int width=Math.min(stage.getWidth(),Math.max(dp(48),Math.round(region[2]-region[0]))),height=Math.min(stage.getHeight(),Math.max(dp(48),Math.round(region[3]-region[1])));
  if(lampSwitch.getLayoutParams().width!=width||lampSwitch.getLayoutParams().height!=height)lampSwitch.setLayoutParams(new FrameLayout.LayoutParams(width,height));
  lampSwitch.setX(Math.max(0,Math.min((region[0]+region[2]-width)*.5f,stage.getWidth()-width)));lampSwitch.setY(Math.max(0,Math.min((region[1]+region[3]-height)*.5f,stage.getHeight()-height)));lampSwitch.setVisibility(View.VISIBLE);
 }
 void updateStudyScene(boolean wide){if(roomExperience!=null)roomExperience.relayout();studyLive.setMotionEnabled(motionEnabled);restartAmbient();}
 void updateLampArtwork(){if(roomExperience!=null)roomExperience.updateLighting();}
 void updateAmbient(){
  if(studyLive==null||weather==null)return;
  WeatherState state=ambientPreview!=null?ambientPreview:weather.state();long now=ambientPreview!=null?ambientPreviewTime:System.currentTimeMillis();
  studyLive.setWeather(state,now);if(roomExperience!=null)roomExperience.setWeather(state,now);
  if(ambientStatus!=null){String updated=state.available?weatherClock(state.observedAt,state):"없음";ambientStatus.setText("날씨 지역: "+weather.place().name+"\n"+weather.status()+"\n"+(state.available?state.description()+" · "+String.format(java.util.Locale.getDefault(),"%.1f°C",state.temperature):"시간만 반영 중")+"\n날씨 기준 시각: "+updated+"\n"+(state.available?"지역 현재 시각: ":"기기 시각: ")+weatherClock(now,state));}
 }
 String weatherClock(long time,WeatherState state){java.text.DateFormat format=android.text.format.DateFormat.getTimeFormat(this);if(state.utcOffsetSeconds!=Integer.MIN_VALUE){java.util.TimeZone zone=java.util.TimeZone.getTimeZone("GMT");zone.setRawOffset(state.utcOffsetSeconds*1000);format.setTimeZone(zone);}return format.format(new java.util.Date(time));}
 void setStudyMotion(boolean enabled){motionEnabled=enabled;getPreferences(0).edit().putBoolean("studyMotion",enabled).apply();if(backups!=null)backups.localChanged();if(studyLive!=null)studyLive.setMotionEnabled(enabled);}
 void setAmbientPreview(WeatherState state,long now){ambientPreview=state;ambientPreviewTime=now;updateAmbient();}
 void clearAmbientPreview(){ambientPreview=null;updateAmbient();}
 void showAmbientSettings(){
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(8),dp(22),dp(12));
  TextView info=label("테마 1에만 적용됩니다. 선택한 도시의 낮·밤과 날씨를 반영하며 위치 권한은 사용하지 않습니다.",13,0xFF574532);content.addView(info);
  Switch motion=new Switch(this);motion.setText("창문 날씨 효과");motion.setMinHeight(dp(48));motion.setChecked(motionEnabled);content.addView(motion);motion.setOnCheckedChangeListener((v,checked)->setStudyMotion(checked));
  Switch quality=new Switch(this);quality.setText("효과 줄이기 · 저사양 기기");quality.setMinHeight(dp(48));quality.setChecked(roomExperience.reduced());content.addView(quality);quality.setOnCheckedChangeListener((v,checked)->roomExperience.setReduced(checked));
  ambientStatus=label("",13,0xFF574532);ambientStatus.setPadding(0,dp(12),0,dp(12));content.addView(ambientStatus);
  Button city=new Button(this);city.setText("날씨 지역 변경");content.addView(city);city.setOnClickListener(v->searchWeatherCity());
  Button refresh=new Button(this);refresh.setText("날씨 새로고침");content.addView(refresh);refresh.setOnClickListener(v->{weather.refresh(true);updateAmbient();});
  TextView credit=label("날씨: Open-Meteo · 도시: GeoNames· 인터넷 연결이 필요합니다. 연결되지 않으면 저장된 정보 또는 시간 기준 조명을 사용합니다.",12,0xFF746650);credit.setPadding(0,dp(10),0,0);content.addView(credit);
  ScrollView scrolling=new ScrollView(this);scrolling.addView(content);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("시간 · 날씨").setView(scrolling).setPositiveButton("닫기",null).create();dialog.setOnDismissListener(v->ambientStatus=null);updateAmbient();dialog.show();
 }
 void searchWeatherCity(){
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("도시 이름 · 예: 서울, 부산, Tokyo");query.setMaxEms(80);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("날씨 지역 검색").setView(query).setNegativeButton("취소",null).setPositiveButton("검색",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String text=query.getText().toString().trim();if(text.length()<2||text.length()>80){query.setError("도시 이름을 2~80자로 입력해 주세요");return;}dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);weather.search(text,(places,error)->{if(isFinishing()||isDestroyed()||!dialog.isShowing())return;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);if(error!=null||places.isEmpty()){query.setError(error!=null?error:"검색 결과가 없어요");return;}String[] names=new String[places.size()];for(int i=0;i<names.length;i++)names[i]=places.get(i).name;new AlertDialog.Builder(this).setTitle("날씨 지역 선택").setItems(names,(d,index)->{weather.setPlace(places.get(index));dialog.dismiss();updateAmbient();}).setNegativeButton("취소",null).show();});}));dialog.show();
 }

  void showBook(int index){
 JSONObject o=sites.optJSONObject(index);if(o==null)return;
 final String url=o.optString("url");
 final String safeUrl=LibraryRules.webUrl(url);
 LinearLayout spread=new LinearLayout(this);spread.setOrientation(LinearLayout.VERTICAL);spread.setPadding(dp(12),dp(12),dp(12),dp(10));spread.setBackgroundColor(theme.paper);
 TextView title=label("📖  "+o.optString("name"),21,0xFF4A2C1C);title.setTypeface(Typeface.SERIF,Typeface.BOLD);spread.addView(title);
 TextView address=label(safeUrl,12,0xFF73513B);address.setPadding(0,dp(8),0,dp(10));address.setOnClickListener(v->edit(index));spread.addView(address);
 LinearLayout bookActions=new LinearLayout(this);
 TextView favorite=label(o.optBoolean("fav")?"★ 즐겨찾기 해제":"☆ 즐겨찾기",14,0xFF674B22);favorite.setPadding(dp(6),dp(10),dp(16),dp(10));bookActions.addView(favorite);
 favorite.setOnClickListener(v->{try{o.put("fav",!o.optBoolean("fav"));favorite.setText(o.optBoolean("fav")?"★ 즐겨찾기 해제":"☆ 즐겨찾기");save();populate();}catch(Exception ignored){}});
 TextView delete=label("삭제",14,0xFF9E392E);delete.setPadding(dp(10),dp(10),dp(10),dp(10));bookActions.addView(delete);spread.addView(bookActions);
 TextView previewTitle=label("사이트 미리보기",14,0xFF6D442A);spread.addView(previewTitle);
 TextView status=label("미리보기를 불러오는 중…",12,0xFF876B52);status.setPadding(0,dp(5),0,dp(6));spread.addView(status);
 WebView web=new WebView(this);web.setBackgroundColor(0xFFFFFFFF);
 web.getSettings().setJavaScriptEnabled(true);
 web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
 web.getSettings().setDomStorageEnabled(true);
 web.getSettings().setLoadsImagesAutomatically(true);
 web.getSettings().setUseWideViewPort(true);
 web.getSettings().setLoadWithOverviewMode(true);
 web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebViewClient(new WebViewClient(){
 @Override public void onPageStarted(WebView view,String u,android.graphics.Bitmap icon){status.setText("미리보기를 불러오는 중…");}
 @Override public void onPageFinished(WebView view,String u){if(status.getText().toString().contains("불러오는 중"))status.setText("미리보기 로딩 완료 · 화면이 비어 있다면 사이트에서 앱 내 표시를 제한할 수 있어요.");}
 @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())status.setText("미리보기를 표시할 수 없어요: "+error.getDescription()+" · 아래 사이트 방문 버튼을 이용하세요.");}
 @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return LibraryRules.webUrl(req.getUrl().toString()).isEmpty();}
 });
 spread.addView(web,new LinearLayout.LayoutParams(-1,dp(260)));
 TextView memoTitle=label("나의 세부사항 · 탭해서 수정",15,0xFF4B3021);memoTitle.setPadding(0,dp(13),0,dp(6));spread.addView(memoTitle);
 TextView memo=label(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"),14,0xFF533D2E);
 memo.setPadding(dp(12),dp(12),dp(12),dp(12));memo.setBackground(bg(0xFFE5D0AA,8));spread.addView(memo);
 memo.setOnClickListener(v->{EditText editor=new EditText(this);editor.setMinLines(4);editor.setGravity(Gravity.TOP);editor.setText(o.optString("notes"));editor.setHint("이 사이트에 관한 나만의 기록, 사용법, 아이디어...");new AlertDialog.Builder(this).setTitle("나의 세부사항 수정").setView(editor).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{try{o.put("notes",editor.getText().toString());save();memo.setText(o.optString("notes").isEmpty()?"＋ 여기를 눌러 메모를 작성하세요.":o.optString("notes"));}catch(Exception e){}}).show();});
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(spread);
 AlertDialog dialog=new AlertDialog.Builder(this).setView(scroll).setNegativeButton("닫기",(d,w)->{}).setNeutralButton("정보 수정",(d,w)->edit(index)).setPositiveButton("사이트 방문",(d,w)->open(safeUrl)).create();
 dialog.setOnDismissListener(d->{web.stopLoading();if(web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)web.getParent()).removeView(web);web.destroy();});
 delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("이 사이트를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{sites.remove(index);save();populate();dialog.dismiss();}).show());
 dialog.show();dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(22),-2);
 if(!safeUrl.isEmpty())web.loadUrl(safeUrl);else status.setText("잘못된 주소입니다. 정보를 수정해 주세요.");
 }
 void open(String url){String safe=LibraryRules.webUrl(url);if(safe.isEmpty()){Toast.makeText(this,"올바른 웹 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(safe)));}catch(Exception e){Toast.makeText(this,"사이트를 열 수 없어요.",Toast.LENGTH_SHORT).show();}}

 void manageCategories(){
 LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(20),dp(8),dp(20),dp(8));
 EditText[] names=new EditText[9];
 for(int i=0;i<9;i++){EditText e=new EditText(this);e.setSingleLine(true);e.setText(categories[i]);e.setHint((i+1)+"번째 칸 이름");layout.addView(e);names[i]=e;}
 ScrollView scroll=new ScrollView(this);scroll.addView(layout);
 new AlertDialog.Builder(this).setTitle("카테고리 이름 수정").setView(scroll).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{
 String[] updated=new String[9];HashSet<String> unique=new HashSet<>();
 for(int i=0;i<9;i++){updated[i]=names[i].getText().toString().trim();if(updated[i].isEmpty()||!unique.add(updated[i].toLowerCase(Locale.ROOT))){Toast.makeText(this,"카테고리 이름은 비어 있거나 중복될 수 없어요.",Toast.LENGTH_LONG).show();return;}}
 String[] previous=categories.clone();
 for(int i=0;i<sites.length();i++){JSONObject o=sites.optJSONObject(i);if(o==null)continue;String old=o.optString("category");for(int j=0;j<9;j++)if(previous[j].equals(old)){try{o.put("category",updated[j]);}catch(Exception ignored){}break;}}
 categories=updated;save();populate();
 }).show();
 }
 boolean handleBookDrop(View target,DragEvent event,int category,int anchor){
 if(!(event.getLocalState() instanceof Integer))return false;
 switch(event.getAction()){
  case DragEvent.ACTION_DRAG_STARTED:return true;
  case DragEvent.ACTION_DRAG_ENTERED:target.setForeground(new android.graphics.drawable.ColorDrawable(0x66F5D48A));return true;
  case DragEvent.ACTION_DRAG_LOCATION:
   int[] location=new int[2],viewport=new int[2];target.getLocationOnScreen(location);shelfScroll.getLocationOnScreen(viewport);
   float y=location[1]+event.getY();long now=SystemClock.uptimeMillis();
   if(now-lastDragScroll>40){if(y<viewport[1]+dp(48))shelfScroll.smoothScrollBy(0,-dp(18));else if(y>viewport[1]+shelfScroll.getHeight()-dp(48))shelfScroll.smoothScrollBy(0,dp(18));lastDragScroll=now;}return true;
  case DragEvent.ACTION_DRAG_EXITED:target.setForeground(null);return true;
  case DragEvent.ACTION_DROP:target.setForeground(null);if(dragSource!=null){dragSource.setAlpha(1);dragSource=null;}moveBookTo((Integer)event.getLocalState(),category,anchor);return true;
  case DragEvent.ACTION_DRAG_ENDED:target.setForeground(null);if(dragSource!=null){dragSource.setAlpha(1);dragSource=null;}return true;
  default:return true;
 }
 }
 void moveBookTo(int index,int category,int anchor){
 JSONObject selected=sites.optJSONObject(index);if(selected==null||index==anchor)return;
 ArrayList<Integer> ordered=new ArrayList<>();
 for(int i=0;i<sites.length();i++){JSONObject o=sites.optJSONObject(i);if(o!=null&&categoryIndex(o.optString("category"))==category)ordered.add(i);}
 sortBooks(ordered);
 List<Integer> moved=LibraryRules.move(ordered,index,anchor,anchor<0);
 try{selected.put("category",categories[category]);for(int i=0;i<moved.size();i++)sites.optJSONObject(moved.get(i)).put("sortOrder",i*1000L);save();int visibleBefore=0;String query=filter.toLowerCase(Locale.ROOT);for(int id:moved){if(id==index)break;JSONObject o=sites.optJSONObject(id);String text=o.optString("name")+" "+o.optString("url")+" "+o.optString("category");if(text.toLowerCase(Locale.ROOT).contains(query))visibleBefore++;}page=visibleBefore/BOOKS_PER_SHELF;populate();}catch(Exception e){Toast.makeText(this,"이동하지 못했어요.",Toast.LENGTH_SHORT).show();}
 }

 void options(int index){JSONObject o=sites.optJSONObject(index);if(o==null)return;String[] choices={o.optBoolean("fav")?"즐겨찾기 해제":"★ 즐겨찾기","책 정보 수정","책장에서 삭제"};new AlertDialog.Builder(this).setTitle(o.optString("name")).setItems(choices,(d,which)->{if(which==0){try{o.put("fav",!o.optBoolean("fav"));save();populate();}catch(Exception e){}}else if(which==1)edit(index);else new AlertDialog.Builder(this).setMessage("이 사이트를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(a,b)->{sites.remove(index);save();populate();}).show();}).setNegativeButton("닫기",null).show();}
 void edit(int index){edit(index,null);}
 void edit(int index,String selectedCategory){JSONObject old=index<0?new JSONObject():sites.optJSONObject(index);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(20),dp(8),dp(20),0);fields.setOrientation(LinearLayout.VERTICAL);
 EditText name=new EditText(this);name.setHint("책 제목 / 사이트 이름");name.setSingleLine();name.setText(old.optString("name"));fields.addView(name);
 EditText url=new EditText(this);url.setHint("사이트 주소 (example.com)");url.setSingleLine();url.setInputType(17);url.setText(old.optString("url"));fields.addView(url);
 Spinner cat=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories);cat.setAdapter(adapter);cat.setSelection(categoryIndex(selectedCategory!=null?selectedCategory:old.optString("category")));fields.addView(cat);
 AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"책장에 사이트 추가":"책 정보 수정").setView(fields).setNegativeButton("취소",null).setPositiveButton("저장",null).create();
 dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{String n=name.getText().toString().trim(),u=url.getText().toString().trim();if(n.isEmpty()||u.isEmpty()){Toast.makeText(this,"제목과 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}u=LibraryRules.webUrl(u);if(u.isEmpty()){Toast.makeText(this,"http 또는 https 웹 주소를 입력해 주세요.",Toast.LENGTH_SHORT).show();return;}try{JSONObject item=new JSONObject();if(old.has("syncId"))item.put("syncId",old.getString("syncId"));else if(index<0)item.put("syncId",java.util.UUID.randomUUID().toString());item.put("name",n);item.put("url",u);item.put("category",categories[cat.getSelectedItemPosition()]);item.put("fav",old.optBoolean("fav"));item.put("notes",old.optString("notes"));item.put("sortOrder",old.optLong("sortOrder",sites.length()*1000L));if(index<0)sites.put(item);else sites.put(index,item);save();populate();dialog.dismiss();}catch(Exception e){Toast.makeText(this,"저장하지 못했어요.",Toast.LENGTH_SHORT).show();}}));dialog.show();
 }
}
