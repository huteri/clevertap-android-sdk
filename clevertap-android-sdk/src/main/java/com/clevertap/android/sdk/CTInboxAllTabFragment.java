package com.clevertap.android.sdk;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * CTInboxAllTabFragment
 */
public class CTInboxAllTabFragment extends CTInboxTabBaseFragment {
    private boolean firstTime = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_all_tab,container,false);
        LinearLayout linearLayout = allView.findViewById(R.id.all_tab_linear_layout);
        //Check if video present to render appropriate recyclerview
        CTInboxMessageAdapter inboxMessageAdapter;
        if(videoPresent) {
            exoPlayerRecyclerView = new ExoPlayerRecyclerView(getActivity());
            exoPlayerRecyclerView.setVisibility(View.VISIBLE);
            exoPlayerRecyclerView.setVideoInfoList(inboxMessageArrayList);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            exoPlayerRecyclerView.setLayoutManager(linearLayoutManager);
            exoPlayerRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            exoPlayerRecyclerView.setItemAnimator(new DefaultItemAnimator());
            inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, getActivity(),this);
            inboxMessageAdapter.filterMessages("all");//Filters the messages before rendering the list on tabs
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
        }else{
            RecyclerView recyclerView = allView.findViewById(R.id.all_tab_recycler_view);
            recyclerView.setVisibility(View.VISIBLE);
            final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, getActivity(),this);
            inboxMessageAdapter.filterMessages("all");//Filters the messages before rendering the list on tabs
            recyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();

        }
        return allView;
    }

    @Override
    public void onPause() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent)
                    exoPlayerRecyclerView.onPausePlayer();
            }
        });
        super.onPause();
    }

    @Override
    public void onResume() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(videoPresent)
                    exoPlayerRecyclerView.onRestartPlayer();
            }
        });
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(exoPlayerRecyclerView!=null && videoPresent)
            exoPlayerRecyclerView.onRelease();
        super.onDestroy();
    }

}