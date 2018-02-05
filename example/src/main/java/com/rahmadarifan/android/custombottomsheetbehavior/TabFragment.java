package com.rahmadarifan.android.custombottomsheetbehavior;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rahmadarifan.library.custombottomsheetbehavior.BottomSheetUtils;

import java.util.ArrayList;
import java.util.List;

public class TabFragment extends Fragment {

    public TabFragment() {
        // Required empty public constructor
    }

    public static TabFragment newInstance() {
        return new TabFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TabPagerAdapter(getChildFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
        BottomSheetUtils.setupViewPager(viewPager);
    }

    class TabPagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> fragments;

        TabPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments = new ArrayList<>();
            fragments.add(ContentTabFragment.newInstance());
            fragments.add(ContentTabFragment.newInstance());
            fragments.add(ContentTabFragment.newInstance());
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Tab " + (position + 1);
        }
    }
}
