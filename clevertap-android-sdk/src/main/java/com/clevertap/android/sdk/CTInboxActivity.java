package com.clevertap.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * CTInboxActivity
 */
public class CTInboxActivity extends FragmentActivity implements CTInboxTabBaseFragment.InboxListener {
    interface InboxActivityListener{
        void messageDidShow(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
        void messageDidClick(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
    }

    private ArrayList<CTInboxMessage> inboxMessageArrayList = new ArrayList<>();
    private CleverTapInstanceConfig config;
    private WeakReference<InboxActivityListener> listenerWeakReference;
    private ExoPlayerRecyclerView exoPlayerRecyclerView;
    private RecyclerView recyclerView;
    private boolean firstTime = true;
    boolean videoPresent = CleverTapAPI.haveVideoPlayerSupport;

    void setListener(InboxActivityListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InboxActivityListener getListener() {
        InboxActivityListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),"InboxActivityListener is null for notification inbox " );
        }
        return listener;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        CTInboxStyleConfig styleConfig;
        try{
            Bundle extras = getIntent().getExtras();
            if(extras == null) throw new IllegalArgumentException();
            styleConfig = extras.getParcelable("styleConfig");
            config = extras.getParcelable("config");
            CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(getApplicationContext(), config);
            if (cleverTapAPI != null) {
                inboxMessageArrayList = cleverTapAPI.getAllInboxMessages();
                setListener(cleverTapAPI);
            }
        }catch (Throwable t){
            Logger.v("Cannot find a valid notification inbox bundle to show!", t);
            return;
        }

        setContentView(R.layout.inbox_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        //noinspection ConstantConditions
        toolbar.setTitle(styleConfig.getNavBarTitle());
        toolbar.setTitleTextColor(Color.parseColor(styleConfig.getNavBarTitleColor()));
        toolbar.setBackgroundColor(Color.parseColor(styleConfig.getNavBarColor()));
        Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        drawable.setColorFilter(Color.parseColor(styleConfig.getBackButtonColor()),PorterDuff.Mode.SRC_IN);
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        LinearLayout linearLayout = findViewById(R.id.inbox_linear_layout);
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
        final TabLayout tabLayout = linearLayout.findViewById(R.id.tab_layout);
        final ViewPager viewPager = linearLayout.findViewById(R.id.view_pager);
        TextView noMessageView = findViewById(R.id.no_message_view);
        //Tabs are shown only if mentioned in StyleConfig
        if(styleConfig.isUsingTabs()){
            viewPager.setVisibility(View.VISIBLE);
            final CTInboxTabAdapter inboxTabAdapter = new CTInboxTabAdapter(getSupportFragmentManager());
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setSelectedTabIndicatorColor(Color.parseColor(styleConfig.getSelectedTabIndicatorColor()));
            tabLayout.setTabTextColors(Color.parseColor(styleConfig.getUnselectedTabColor()),Color.parseColor(styleConfig.getSelectedTabColor()));
            tabLayout.setBackgroundColor(Color.parseColor(styleConfig.getTabBackgroundColor()));
            tabLayout.addTab(tabLayout.newTab().setText("ALL"));

            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("inboxMessages", inboxMessageArrayList);
            bundle.putParcelable("config", config);
            bundle.putParcelable("styleConfig", styleConfig);
            CTInboxAllTabFragment all = new CTInboxAllTabFragment();
            all.setArguments(bundle);
            inboxTabAdapter.addFragment(all,"ALL");

            if(styleConfig.getFirstTab() != null && !styleConfig.getFirstTab().isEmpty()) {
                CTInboxAllTabFragment first = new CTInboxAllTabFragment();
                first.setArguments(bundle);
                inboxTabAdapter.addFragment(first,styleConfig.getFirstTab());
                viewPager.setOffscreenPageLimit(1);
            }


            if(styleConfig.getSecondTab() != null && !styleConfig.getSecondTab().isEmpty()) {
                CTInboxAllTabFragment second = new CTInboxAllTabFragment();
                bundle = (Bundle)bundle.clone();
                second.setArguments(bundle);
                inboxTabAdapter.addFragment(second,styleConfig.getSecondTab());
                viewPager.setOffscreenPageLimit(2);
            }

            viewPager.setAdapter(inboxTabAdapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    CTInboxTabBaseFragment fragment = (CTInboxTabBaseFragment) inboxTabAdapter.getItem(tab.getPosition());
                    if(((CTInboxAllTabFragment) fragment).exoPlayerRecyclerView!=null){
                        ((CTInboxAllTabFragment) fragment).exoPlayerRecyclerView.playVideo();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    CTInboxTabBaseFragment fragment = (CTInboxTabBaseFragment) inboxTabAdapter.getItem(tab.getPosition());
                    if(((CTInboxAllTabFragment) fragment).exoPlayerRecyclerView!=null){
                        ((CTInboxAllTabFragment) fragment).exoPlayerRecyclerView.stop();
                    }
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            tabLayout.setupWithViewPager(viewPager);
        }else{
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            //ExoPlayerRecyclerView manages autoplay of videos on scoll and hence only used if Inbox messages contain videos
            CTInboxMessageAdapter inboxMessageAdapter;
            if(videoPresent) {
                exoPlayerRecyclerView = new ExoPlayerRecyclerView(getApplicationContext());
                exoPlayerRecyclerView.setVisibility(View.VISIBLE);
                exoPlayerRecyclerView.setVideoInfoList(inboxMessageArrayList);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                exoPlayerRecyclerView.setLayoutManager(linearLayoutManager);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(exoPlayerRecyclerView.getContext(),
                        linearLayoutManager.getOrientation());
                exoPlayerRecyclerView.addItemDecoration(dividerItemDecoration);
                exoPlayerRecyclerView.setItemAnimator(new DefaultItemAnimator());

                inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, this,null);
                exoPlayerRecyclerView.setAdapter(inboxMessageAdapter);
                inboxMessageAdapter.notifyDataSetChanged();
                if (firstTime) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            exoPlayerRecyclerView.playVideo();
                        }
                    },1000);
                    firstTime = false;
                }
                linearLayout.addView(exoPlayerRecyclerView);
                noMessageView.setVisibility(View.GONE);
            }else{//Normal Recycler view in case inbox messages don't contain any videos
                recyclerView = findViewById(R.id.activity_recycler_view);
                recyclerView.setVisibility(View.VISIBLE);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(linearLayoutManager);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                        linearLayoutManager.getOrientation());
                recyclerView.addItemDecoration(dividerItemDecoration);
                recyclerView.setItemAnimator(new DefaultItemAnimator());

                inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, this,null);
                recyclerView.setAdapter(inboxMessageAdapter);
                inboxMessageAdapter.notifyDataSetChanged();
                noMessageView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        didShow(data,inboxMessage);
    }

    @Override
    public void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        didClick(data,inboxMessage);
    }

    void didClick(Bundle data, CTInboxMessage inboxMessage) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(this,inboxMessage, data);
        }
    }

    void didShow(Bundle data, CTInboxMessage inboxMessage) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidShow(this,inboxMessage, data);
        }
    }

    /**
     * Handles click of inbox message CTA button
     * @param position int row in the RecyclerView
     * @param buttonText  String text of the Button
     */
    void handleClick(int position, String buttonText, JSONObject jsonObject){
        try {
            Bundle data = new Bundle();

            //data.putString(Constants.NOTIFICATION_ID_TAG,inboxMessageArrayList.get(position).getCampaignId());
            JSONObject wzrkParams = inboxMessageArrayList.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while(iterator.hasNext()){
                String keyName = iterator.next();
                if(keyName.startsWith(Constants.WZRK_PREFIX))
                    data.putString(keyName,wzrkParams.getString(keyName));
            }

            if(buttonText != null && !buttonText.isEmpty())
                data.putString("wzrk_c2a", buttonText);
            didClick(data,inboxMessageArrayList.get(position));

           if (jsonObject != null) {
                if(inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getLinktype(jsonObject).equalsIgnoreCase(Constants.COPY_TYPE)){
                    // noinspection UnnecessaryReturnStatement
                    return;
                }else{
                    String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getLinkUrl(jsonObject);
                    if (actionUrl != null) {
                        fireUrlThroughIntent(actionUrl);
                    }
                }
            }else {
                String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getActionUrl();
                if (actionUrl != null) {
                    fireUrlThroughIntent(actionUrl);
                }
}
        } catch (Throwable t) {
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    /**
     * Handles click of inbox message carousel view pager
     * @param position int row in the RecyclerView
     * @param viewPagerPosition int position of the ViewPager
     */
    void handleViewPagerClick(int position, int viewPagerPosition){
        try {
            Bundle data = new Bundle();

            JSONObject wzrkParams = inboxMessageArrayList.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while(iterator.hasNext()){
                String keyName = iterator.next();
                if(keyName.startsWith(Constants.WZRK_PREFIX))
                    data.putString(keyName,wzrkParams.getString(keyName));
            }

            didClick(data,inboxMessageArrayList.get(position));
            String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(viewPagerPosition).getActionUrl();
            fireUrlThroughIntent(actionUrl);
        }catch (Throwable t){
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    void fireUrlThroughIntent(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
    }

    @Override
    public void onPause() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent) {
                    if(exoPlayerRecyclerView !=null)
                        exoPlayerRecyclerView.onPausePlayer();
                }
            }
        });
        super.onPause();
    }

    @Override
    public void onResume() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent) {
                    if(exoPlayerRecyclerView !=null)
                        exoPlayerRecyclerView.onRestartPlayer();
                }
            }
        });
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(exoPlayerRecyclerView!=null && videoPresent)
            exoPlayerRecyclerView.release();
        super.onDestroy();
    }
}
