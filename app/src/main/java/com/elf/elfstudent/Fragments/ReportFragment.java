package com.elf.elfstudent.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.elf.elfstudent.Activities.SingleSubjectReportActivity;
import com.elf.elfstudent.Adapters.LessonListAdapter;
import com.elf.elfstudent.Adapters.ReportLessonAdapter;
import com.elf.elfstudent.DataStorage.DataStore;
import com.elf.elfstudent.Network.AppRequestQueue;
import com.elf.elfstudent.Network.ErrorHandler;
import com.elf.elfstudent.Network.JsonProcessors.LessonProvider;
import com.elf.elfstudent.R;
import com.elf.elfstudent.Utils.BundleKey;
import com.elf.elfstudent.Utils.RequestParameterKey;
import com.elf.elfstudent.Utils.ScreenUtil;
import com.elf.elfstudent.model.Lesson;
import com.elf.elfstudent.model.Topic;

import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.support.v4.app.ActivityOptionsCompat.*;
import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static com.elf.elfstudent.Adapters.ReportLessonAdapter.*;

/**
 * Created by nandhu on 20/10/16.
 * The Fragment which is provided by {@link com.elf.elfstudent.Adapters.ViewPagerAdapters.ReportPagerAdapter}
 *
 * Gets The OVerall Percentage , lesson List and shows them in lIst
 * clicking on List , will show them topic wise
 */
public class ReportFragment extends Fragment implements LessonClickCallbacks, LessonProvider.SubjectLoaderCallback, ErrorHandler.ErrorHandlerCallbacks {


    private static final String TAG = "REPORT_FRAG";
    private static final String REPORT_URL = "http://www.hijazboutique.com/elf_ws.svc/GetLessionWiseReport";



    @BindView(R.id.report_frag_card)
    CardView mTopRoot;

    @BindView(R.id.report_frag_list)
    RecyclerView mList;

    //The Top Level Content Root



    String overall = "";

    private List<Topic> mTopiclist = null;
//    List<T> mLessonNames;

    ReportLessonAdapter mAdapter = null;

    List<Lesson> mLessonList = null;

    String subjecId = null;
    DataStore mStore = null;

    JsonArrayRequest getLessonRequest = null;
    private String subjectName = null;
    String studentId = null;

    private AppRequestQueue mRequestQueue = null;
    private LessonProvider mLessonProvider = null;
    private ErrorHandler errorHandler = null;

    @BindView(R.id.report_progress)
    FrameLayout mLoadingLayout;

    @BindView(R.id.report_visisble_layout)
    RelativeLayout mVisibleLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.report_fragment, container, false);
        ButterKnife.bind(this, mView);
        if (getArguments() != null) {
            subjecId = getArguments().getString(BundleKey.SUBJECT_ID);
            subjectName = getArguments().getString(BundleKey.SUBJECT_NAME);

        }

        mRequestQueue = AppRequestQueue.getInstance(getContext());
        mLessonProvider = new LessonProvider(this);
        errorHandler = new ErrorHandler(this);
        mStore = DataStore.getStorageInstance(getContext());

        if (mStore != null) {
            studentId = mStore.getStudentId();
        }

        if (studentId != null) {
            PrepareSubjectReportsFor(studentId,subjecId);

        }




//

        return mView;

    }
    private void PrepareSubjectReportsFor(String studentId, String subjecId) {
        JSONObject mObject = new JSONObject();
        try {
            mObject.put("studentId", "1");
            mObject.put("subjectId", "11");
        } catch (Exception e) {
            Log.d(TAG, "PrepareSubjectReportsFor: ");
        }

        getLessonRequest = new JsonArrayRequest(Request.Method.POST, REPORT_URL, mObject, mLessonProvider, errorHandler);

        if (mRequestQueue != null) {
            mRequestQueue.addToRequestQue(getLessonRequest);

        }
    }






    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void ShowLessonReportFor(int position, LessonView itemView) {


        Log.d("REport", "ShowLessonReportFor: ");
        final Intent i = new Intent(getActivity(), SingleSubjectReportActivity.class);
        //i.putExtra(BundleKey.LESSON_ID,mLessonList.get(position).getLessonId());

        String LessonTransName = ViewCompat.getTransitionName(itemView.mLessonName);
        String percentTransName = ViewCompat.getTransitionName(itemView.mGrowth);
        i.putExtra(BundleKey.SUBJECT_ID,subjecId);
        i.putExtra(BundleKey.LESSON_ID,mLessonList.get(position).getLessonId());
        i.putExtra(BundleKey.LESSON_NAME,mLessonList.get(position).getmLessonName());
        i.putExtra(BundleKey.PERCENTAGE,mLessonList.get(position).getmGrowthPercentage());
        i.putExtra(BundleKey.LESSON_NAME_TRANS,LessonTransName);
        i.putExtra(BundleKey.PERCENT_TRANS,percentTransName);
        //MAking pairs for many Share elements

        Pair<View, String> p2 = Pair.create((View) itemView.mLessonName, LessonTransName);
        Pair<View, String> p3 = Pair.create((View)itemView.mGrowth, percentTransName);


        ActivityOptionsCompat options = makeSceneTransitionAnimation(getActivity(), p2,p3);
        if (ScreenUtil.isAndroid5()){

            startActivity(i, options.toBundle());
        }else{
            startActivity(i);
        }
    }

    @Override
    public void setLessonList(List<Lesson> mLessons) {
        this.mLessonList = mLessons;

        mLoadingLayout.setVisibility(View.INVISIBLE);
        mVisibleLayout.setVisibility(View.VISIBLE);
        Log.d(TAG, "got lesson list");
        mAdapter = new ReportLessonAdapter(getContext(),mLessons,this);
        mList.setLayoutManager(new LinearLayoutManager(getContext()));
        mList.setAdapter(mAdapter);
    }

    @Override
    public void noLesson() {

    }

    @Override
    public void TimeoutError() {
        Log.d(TAG, "TimeoutError: ");

    }

    @Override
    public void NetworkError() {
        Log.d(TAG, "NetworkError: ");
    }

    @Override
    public void ServerError() {
        Log.d(TAG, "ServerError: ");
    }
}
