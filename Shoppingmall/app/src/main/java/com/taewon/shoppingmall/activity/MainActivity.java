package com.taewon.shoppingmall.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.taewon.shoppingmall.R;
import com.taewon.shoppingmall.User;
import com.taewon.shoppingmall.adapter.AdsViewPagerAdapter;
import com.taewon.shoppingmall.adapter.BoardRecyclerAdapter;
import com.taewon.shoppingmall.adapter.MiniBoardRecyclerAdapter;
import com.taewon.shoppingmall.adapter.UserProfileRecyclerAdapter;
import com.taewon.shoppingmall.dialog.LottieLoadingDialog;
import com.taewon.shoppingmall.item.AdsItem;
import com.taewon.shoppingmall.item.BoardItem;
import com.taewon.shoppingmall.util.BoardDateComparator;
import com.taewon.shoppingmall.util.PreferenceMgr;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    ArrayList<AdsItem> adsItems;
    Timer adsTimer;
    ArrayList<BoardItem> boardItems;

    BottomNavigationView bottomNavigationView;
    int currentAdsPage = 0;

    DrawerLayout drawerLayout;
    EditText et_search;

    ArrayList<BoardItem> items2D;
    ArrayList<BoardItem> items3D;
    ArrayList<User> userItems;

    ImageView iv_2dDetail;
    ImageView iv_3dDetail;
    ImageView iv_planDetail;
    LinearLayout li_2d_detail_category;
    LinearLayout li_3d_detail_category;
    LinearLayout li_plan_detail_category;
    LottieLoadingDialog loadingDialog;

    FirebaseDatabase database;
    FirebaseAuth mAuth;
    FirebaseStorage storage;

    UserProfileRecyclerAdapter userProfileRecyclerAdapter;
    AdsViewPagerAdapter pagerAdapter;
    MiniBoardRecyclerAdapter boardRecyclerAdapter2D;
    MiniBoardRecyclerAdapter boardRecyclerAdapter3D;


    RecyclerView rv_main_todayDesigner;
    RecyclerView rv_newest2D;
    RecyclerView rv_newest3D;

    ViewPager2 vp_adsViewPager;
    LinearLayout wrap_layout;
    LinearLayout li_logout;


    ImageView iv_rightDrawerUserImg;
    TextView tv_rightDrawerUserName;

    final Handler handler = new Handler();
    final Runnable Update = new Runnable() {
        public void run() {
            if (currentAdsPage == adsItems.size()) {
                currentAdsPage = 0;
            }
            ViewPager2 viewPager2 = vp_adsViewPager;
            int i = currentAdsPage;
            currentAdsPage = i + 1;
            viewPager2.setCurrentItem(i, true);
        }
    };
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        adsItems = new ArrayList<>();
        boardItems = new ArrayList<>();
        items2D = new ArrayList<>();
        items3D = new ArrayList<>();
        items2D = new ArrayList<>();
        items3D = new ArrayList<>();
        userItems = new ArrayList<>();

        loadingDialog = new LottieLoadingDialog(this);

        initViews();
        initListeners();
        initDrawers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getUser();
        getAds();
        getBoard();
        setTodayDesigner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        adsTimer.cancel();
    }

    public void getUser(){
        database.getReference("Users").child(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                User item = dataSnapshot.getValue(User.class);
                if(item != null){
                    storage.getReference(item.getPhotoUrl()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(MainActivity.this)
                                    .load(uri)
                                    .error(R.drawable.ic_warning)
                                    .into(iv_rightDrawerUserImg);
                            tv_rightDrawerUserName.setText(item.getUsername());
                        }
                    });
                }
                else{
                    Toast.makeText(MainActivity.this, "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void getBoard() {
        loadingDialog.show();
        database.getReference("Board/").limitToFirst(20).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            public void onSuccess(DataSnapshot dataSnapshot) {
                // 1.데이터는 쌓인다. 청소하자.
                boardItems.clear();
                items2D.clear();
                items3D.clear();

                // 2. 가져온 보드들을 우선 모두 리스트에 정리하고,
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BoardItem item = (BoardItem) snapshot.getValue(BoardItem.class);
                    item.setBoardID(snapshot.getKey());
                    boardItems.add(item);
                }

                // 3. Iterator를 이용해 2d 카테고리와 3d 카테고리를 분리.
                Iterator<BoardItem> it = boardItems.iterator();
                while (it.hasNext()) {
                    BoardItem item = it.next();
                    List<String> tag = item.getTags();
                    if (tag.contains("2d") || tag.contains("2d.*")) {
                        //2d
                        items2D.add(item);
                    } else if (tag.contains("3d") || tag.contains("3d ")) {
                        //3d
                        items3D.add(item);
                    }

                    if(items2D.size() > 3 && items3D.size() > 3){
                        break;
                    }
                }
                
                // 4. 각 2d, 3d 아이템들을 최신순으로 정렬.
                Collections.sort(items2D, new BoardDateComparator());
                Collections.sort(items3D, new BoardDateComparator());
                
                // 5. 어댑터에 리스트 데이터 구조가 바뀌었음을 알려주자.
                boardRecyclerAdapter2D.notifyDataSetChanged();
                boardRecyclerAdapter3D.notifyDataSetChanged();
                
                // 6. 로딩창 닫기
                loadingDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            // 연결에 실패했을 때.
            public void onFailure(Exception e) {
                loadingDialog.dismiss();
                new AlertDialog.Builder(MainActivity.this).setTitle("게시글을 불러오지 못했습니다. 다시 시도해보세요.")
                        .setMessage("")
                        .setIcon(R.drawable.ic_baseline_back_hand_24)
                        .setPositiveButton("다시 시도", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getBoard();
                    }
                }).setNegativeButton("취소", (DialogInterface.OnClickListener) null).create().show();
            }
        });
    }

    private void setTodayDesigner() {
        DatabaseReference databaseRef = database.getReference("Users");
        databaseRef.get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                userItems.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    User instance = ds.getValue(User.class);
                    if(instance.isDesigner()){
                        userItems.add(instance);
                    }
                    if(userItems.size() == 3){
                        break;
                    }
                }
                rv_main_todayDesigner.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
                userProfileRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void getAds() {
        database.getReference().child("Ads").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            public void onComplete(Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "로딩 오류", Toast.LENGTH_SHORT).show();
                    return;
                }
                adsItems.clear();
                Map<String, Object> map = (Map) task.getResult().getValue();
                for (String keys : map.keySet()) {
                    Map<String, String> child = (Map) map.get(keys);
                    StorageReference storageRef = storage.getReference(child.get("image"));
                    adsItems.add(new AdsItem(storageRef.getPath(), child.get("ref")));
                }
                pagerAdapter.notifyDataSetChanged();
            }
        });
        adsTimer = new Timer();
        adsTimer.schedule(new TimerTask() {
            public void run() {
                handler.post(Update);
            }
        }, 500, 3000);
    }

    /* access modifiers changed from: private */
    public void openCategoryDrawer() {
        drawerLayout.openDrawer((int) GravityCompat.START);
        wrap_layout.setClickable(false);
        wrap_layout.setEnabled(false);
    }

    /* access modifiers changed from: private */
    public void openMyInfoDrawer() {
        drawerLayout.openDrawer((int) GravityCompat.END);
        wrap_layout.setClickable(false);
        wrap_layout.setEnabled(false);
    }

    public void insertBoardTest() {
        DatabaseReference upload = database.getReference("Board/").push();
        StorageReference storageRef = storage.getReference("Board/");
        new ArrayList();
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Bitmap bitmap = ((BitmapDrawable) getDrawable(R.drawable.example)).getBitmap();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, bos);
            storageRef.child(upload.getKey()).child(Integer.toString(i)).putBytes(bos.toByteArray()).addOnCompleteListener((OnCompleteListener) new OnCompleteListener<UploadTask.TaskSnapshot>() {
                public void onComplete(Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d("이미지 업로드", task.getResult().toString());
                    }
                }
            });
        }
        tags.add("2d");
        tags.add("2d 배경");
        tags.add("2d 디자인");
        upload.setValue(new BoardItem("ANpRQjoMP5dXQQZEO6iFJigeQBs2", "김태원", "3번째", "@@@@@@@@", tags, getDate()));
    }

    private String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = Calendar.getInstance().getTime();
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return sdf.format(date);
    }

    private void initViews() {
        li_logout = findViewById(R.id.li_logout);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        et_search = findViewById(R.id.et_search);
        wrap_layout = (LinearLayout) findViewById(R.id.wrap_layout);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomnav_bottom_menu);
        vp_adsViewPager = (ViewPager2) findViewById(R.id.vp_adsViewPager);
        rv_main_todayDesigner = findViewById(R.id.rv_main_todayDesigner);
        rv_newest2D = (RecyclerView) findViewById(R.id.rv_newest2D);
        rv_newest3D = (RecyclerView) findViewById(R.id.rv_newest3D);

        userProfileRecyclerAdapter = new UserProfileRecyclerAdapter(MainActivity.this, userItems);
        rv_main_todayDesigner.setAdapter(userProfileRecyclerAdapter);

        pagerAdapter = new AdsViewPagerAdapter(this, adsItems);
        vp_adsViewPager.setAdapter(pagerAdapter);

        boardRecyclerAdapter2D = new MiniBoardRecyclerAdapter(MainActivity.this, items2D);
        rv_newest2D.setAdapter(boardRecyclerAdapter2D);
        rv_newest2D.setLayoutManager(new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false));
        new PagerSnapHelper().attachToRecyclerView(rv_newest2D);

        boardRecyclerAdapter3D = new MiniBoardRecyclerAdapter(MainActivity.this, items3D);
        rv_newest3D.setAdapter(boardRecyclerAdapter3D);
        rv_newest3D.setLayoutManager(new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false));
        new PagerSnapHelper().attachToRecyclerView(rv_newest3D);
    }

    private void initListeners() {
        et_search.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    et_search.clearFocus();
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                    intent.putExtra("BoardItems", boardItems);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                }
            }
        });
        findViewById(R.id.iv_myinfo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openMyInfoDrawer();
            }
        });
        findViewById(R.id.iv_category).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openCategoryDrawer();
            }
        });
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.bottom_category :
                        openCategoryDrawer();
                        return true;
                    case R.id.bottom_myinfo :
                        openMyInfoDrawer();
                        return true;
                    case R.id.bottom_search :
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void initDrawers() {
        li_2d_detail_category = (LinearLayout) findViewById(R.id.li_2d_detail_category);
        li_3d_detail_category = (LinearLayout) findViewById(R.id.li_3d_detail_category);
        li_plan_detail_category = (LinearLayout) findViewById(R.id.li_plan_detail_category);
        iv_2dDetail = (ImageView) findViewById(R.id.iv_2dDetail);
        iv_3dDetail = (ImageView) findViewById(R.id.iv_3dDetail);
        iv_planDetail = (ImageView) findViewById(R.id.iv_planDetail);
        li_2d_detail_category.setVisibility(View.GONE);
        li_3d_detail_category.setVisibility(View.GONE);
        li_plan_detail_category.setVisibility(View.GONE);


        //왼쪽 드로워
        drawerLayout.findViewById(R.id.iv_leftDrawerCancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                drawerLayout.closeDrawer((int) GravityCompat.START);
            }
        });
        drawerLayout.findViewById(R.id.li_2dCategory).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (li_2d_detail_category.getVisibility() == View.GONE) {
                    li_2d_detail_category.setVisibility(View.VISIBLE);
                    iv_2dDetail.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
                    return;
                }
                li_2d_detail_category.setVisibility(View.GONE);
                iv_2dDetail.setImageResource(R.drawable.ic_baseline_arrow_right_24);
            }
        });
        drawerLayout.findViewById(R.id.li_3dCategory).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (li_3d_detail_category.getVisibility() == View.GONE) {
                    li_3d_detail_category.setVisibility(View.VISIBLE);
                    iv_3dDetail.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
                    return;
                }
                li_3d_detail_category.setVisibility(View.GONE);
                iv_3dDetail.setImageResource(R.drawable.ic_baseline_arrow_right_24);
            }
        });
        drawerLayout.findViewById(R.id.li_plannerCategory).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (li_plan_detail_category.getVisibility() == View.GONE) {
                    li_plan_detail_category.setVisibility(View.VISIBLE);
                    iv_planDetail.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
                    return;
                }
                li_plan_detail_category.setVisibility(View.GONE);
                iv_planDetail.setImageResource(R.drawable.ic_baseline_arrow_right_24);
            }
        });
        drawerLayout.findViewById(R.id.tv_2d_character).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("2d 캐릭터");
            }
        });
        drawerLayout.findViewById(R.id.tv_2d_background).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("2d 배경");
            }
        });
        drawerLayout.findViewById(R.id.tv_2d_animation).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("2d 애니메이션");
            }
        });
        drawerLayout.findViewById(R.id.tv_3d_character).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("3d 캐릭터");
            }
        });
        drawerLayout.findViewById(R.id.tv_3d_animation).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("3d 애니메이션");
            }
        });
        drawerLayout.findViewById(R.id.tv_3d_background).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("3d 배경");
            }
        });
        drawerLayout.findViewById(R.id.tv_3d_modeling).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("3d 모델링");
            }
        });
        drawerLayout.findViewById(R.id.tv_plan_gameIdea).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("게임아이디어");
            }
        });
        drawerLayout.findViewById(R.id.tv_plan_bgMusic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("배경음악");
            }
        });
        drawerLayout.findViewById(R.id.tv_plan_levelDesign).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("레벨디자인");
            }
        });
        drawerLayout.findViewById(R.id.tv_plan_effectSound).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                categoryIntent("효과음");
            }
        });

        //오른쪽 드로워
        iv_rightDrawerUserImg = drawerLayout.findViewById(R.id.iv_rightDrawerUserImg);
        tv_rightDrawerUserName = drawerLayout.findViewById(R.id.tv_rightDrawerUserName);

        drawerLayout.findViewById(R.id.iv_rightDrawerCancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                drawerLayout.closeDrawer((int) GravityCompat.END);
            }
        });
        drawerLayout.findViewById(R.id.li_salesRegistrationBtn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        drawerLayout.findViewById(R.id.li_saleItems).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        drawerLayout.findViewById(R.id.li_shoppingBasket).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        drawerLayout.findViewById(R.id.li_appInfo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        drawerLayout.findViewById(R.id.li_serviceCenter).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        drawerLayout.findViewById(R.id.li_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        findViewById(R.id.btn_insertTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                insertBoardTest();
            }
        });
    }

    private void logout(){
        mAuth.signOut();
        PreferenceMgr.removeKey(MainActivity.this, "AutoLogin");
        PreferenceMgr.removeKey(MainActivity.this, "id");
        PreferenceMgr.removeKey(MainActivity.this, "pw");
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    /* access modifiers changed from: private */
    public void categoryIntent(String category) {
        Intent intent = new Intent(this, MainActivity2.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }
}
