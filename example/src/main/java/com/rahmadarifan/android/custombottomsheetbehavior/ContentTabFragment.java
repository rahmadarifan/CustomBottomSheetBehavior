package com.rahmadarifan.android.custombottomsheetbehavior;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ContentTabFragment extends Fragment {
    public ContentTabFragment() {
        // Required empty public constructor
    }

    public static ContentTabFragment newInstance() {
        return new ContentTabFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_content_tab, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ContentTabAdapter(creatExampleData()));
        return view;
    }

    private List<String> creatExampleData() {
        List<String> examples = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            examples.add("Example " + (i + 1));
        }
        return examples;
    }
}
