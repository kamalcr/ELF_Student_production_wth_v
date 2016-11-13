package com.elf.elfstudent.Activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.elf.elfstudent.Adapters.QuestionPagerAdapter;
import com.elf.elfstudent.DataStorage.DataStore;
import com.elf.elfstudent.Network.AppRequestQueue;
import com.elf.elfstudent.Network.ErrorHandler;
import com.elf.elfstudent.Network.JsonProcessors.QuestionProvider;
import com.elf.elfstudent.Network.JsonProcessors.TestSubitter;
import com.elf.elfstudent.R;
import com.elf.elfstudent.Utils.BundleKey;
import com.elf.elfstudent.Utils.TimerMenu;
import com.elf.elfstudent.model.Answers;
import com.elf.elfstudent.model.Question;
import com.elf.elfstudent.model.TestSubmit;
import com.gigamole.navigationtabstrip.NavigationTabStrip;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

/**
 * Created by nandhu on 18/10/16.
 *
 * The page Which Pulls The Test Question According to
 *
 * {@param testiD from Intent and Displays Them in View pager}
 */

public class TestWriteActivity extends AppCompatActivity implements ErrorHandler.ErrorHandlerCallbacks, QuestionProvider.QuestionCallback, TestSubitter.SubmittedTestCallback {
    private static final String TAG = "TestWritePage";
    private static final String TEST_SUBMIT = "http://www.hijazboutique.com/elf_ws.svc/SubmitTest";
    private static final String GET_QUESTIONS_URL = "http://www.hijazboutique.com/elf_ws.svc/GetTestQuestions";


    //The Views


    @BindView(R.id.test_write_root)
    FrameLayout mChangableRoot;
    //Top Tab
    @BindView(R.id.nts_center)
    NavigationTabStrip mTab;



    //The Pager, which Displays question one by one
    @BindView(R.id.test_write_viewpager)
    ViewPager mPager;

    //The finish Test Button
    @BindView(R.id.test_write_finish)
    Button testWriteFinish;



    //The Adapter which Shows Question One by one
    private QuestionPagerAdapter mAdapter = null;

    //The Total No of Questions Count for this Test iD obtained from Making Request
    private int mQuestionCount = 0;
    private List<Question> mQuestionList = new ArrayList<>();



    //The Object Which Provdies student Details
    private DataStore mStore;


    //The Request Queue for This Activity

    private AppRequestQueue mRequestQueue = null;

    private ProgressDialog mBar;

    QuestionProvider mQuestionProvider = null;


    //Single Test Id for WHich Question Are Pulled and SHowed
    private String mTestId = null;
    private String mSubjectId = null;
    private String mSubjectName = null;
    private String mTestDesc = null;
    private String mStudentID = null;


    //Error Handler for Volley
    ErrorHandler errorHandler ;

    TestSubitter mTestSubmiter = null;

    //The Request Objects
    private JsonArrayRequest getQuestionRequest = null;
    private JsonArrayRequest submitTestRequest = null;
    private int count = 0;
    private long timer = 1000 * 60 *20;  //20minutes

    FirebaseAnalytics mAnalytics  = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_write_activity);
        ButterKnife.bind(this);


        //initialise Request Que
        mRequestQueue = AppRequestQueue.getInstance(this);
        mStore = DataStore.getStorageInstance(this);

        mBar = new ProgressDialog(this);
        mBar.setIndeterminate(true);
        mBar.setTitle("Getting Questions");
        mBar.show();



        if (mStore != null){
            mStudentID = mStore.getStudentId();
        }


        //get Test Details From Intent From Intent
        if (getIntent() != null){

            mTestId = getIntent().getStringExtra(BundleKey.TEST_ID);
            mSubjectId = getIntent().getStringExtra(BundleKey.SUBJECT_ID);
            mSubjectName  =getIntent().getStringExtra(BundleKey.SUBJECT_NAME);
            mTestDesc = getIntent().getStringExtra(BundleKey.TEST_DESC);
        }
        else{
            throw new NullPointerException("TestID cannot be Null");
        }


        //get Question From webServices , until then
        //SHow progress Dialog or Some Animation
        mQuestionProvider = new QuestionProvider(this);
        errorHandler = new ErrorHandler(this);
        mTestSubmiter  = new TestSubitter(this);



        if (mTestId != null && mSubjectId != null) {

            prepareTestQuestionsFor(mTestId,mSubjectId);
        }
        else {

            //NO Test PAge Noticed
             FirebaseCrash.log("NO Request is being Passed");
            mChangableRoot.removeAllViews();
            View v = View.inflate(this,R.layout.no_data,mChangableRoot);
        }

        //The Button which Completed the Test
        testWriteFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


               finishButtonClicked();

            }
        });

    }

    private void finishButtonClicked() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Finish Test?");
        alertDialog.setMessage("Are you sure  you want to finish the test");
        alertDialog.setPositiveButton("COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishTest();
            }
        });
        alertDialog.setNegativeButton("STAY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //No Has Been Clicked, Dismiss Dialog


            }
        });
        alertDialog.show();
    }

    /*This method Gets the Question Clicked SO for From
    { @link QuestionPagerAdapter and Sends to adapter for validation*/

    private void finishTest() {
        if(mAdapter != null){


            try {

                List<Question> answerList =  mAdapter.getQuestionList();
                JSONObject obj = null;
                JSONArray mArray = new JSONArray();

                int count = answerList.size();

                //The Rest Model That is sent to server
                //Adding selected option From Pager Adapter data
                TestSubmit testSubmit = new TestSubmit(mStudentID,mTestId,count);
                for (int i= 0; i<count ; i++){
                    obj = new JSONObject();
                    obj.put("QuestionId",answerList.get(i).getmQuestionId());
                    obj.put("AnswerSelected",answerList.get(i).getSelectedOption());

                    mArray.put(obj);
                    obj = null;

                }


                //Adding Student Id and test Id to

                testSubmit.setStudnetID(mStore.getStudentId());
                testSubmit.setTestId(mTestId);
                SubmitTest(testSubmit,count,mArray);
            }
                catch (Exception e){
                  FirebaseCrash.log("Finish Test Exception "+e.getLocalizedMessage());
                }
        }
    }

    private void SubmitTest(TestSubmit testSubmit, int count, JSONArray mArray) {

        JSONArray testArray = new JSONArray();
        final JSONObject testObject = new JSONObject();
        testArray.put(testSubmit.getQuestionSet());

        try {



            //preapare test Object
            testObject.put("StudentId",testSubmit.getStudnetID());
            testObject.put("TestId",testSubmit.getTestId());
            testObject.put("Answers",mArray);

            Log.d(TAG, "Submit Test Rquest body "+testObject.toString());
            //send Request
            submitTestRequest = new JsonArrayRequest(Request.Method.POST, TEST_SUBMIT, testObject, mTestSubmiter,errorHandler);

            if (mRequestQueue != null){

                mRequestQueue.addToRequestQue(submitTestRequest);
            }
            else{
                testNotSubmitted();
            }

        }


        catch (Exception e){

            FirebaseCrash.log("Test Not Submitted in "+e.getLocalizedMessage());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: ");

        return true;
    }


    /*This Method hits the webserivice and gets the Question*/

    private void prepareTestQuestionsFor(final String mTestId, final String mSubjectId) {

        //contstruct Request Parameters
        final JSONObject mObject = new JSONObject();

        try {
            mObject.put("StudentId", mStudentID);
            mObject.put("TestId", mTestId);
            mObject.put("SubjectId", mSubjectId);
        } catch (Exception e) {
            FirebaseCrash.log("Exception in Putting  Test Request JSOn Object");
        }

        //Request Object
          getQuestionRequest = new JsonArrayRequest(Request.Method.POST, GET_QUESTIONS_URL, mObject, mQuestionProvider, errorHandler);

        getQuestionRequest.setTag("QUESTION");
        if (mRequestQueue != null){

            mRequestQueue.addToRequestQue(getQuestionRequest);
        }
        else{
            throw new NullPointerException("Request Queue cannot be Null");
        }


    }



    //set Adapter data and Tab
    private void setAdapter(String[] mTitles, List<Question> mQuestionList) {

            Log.d(TAG, "onResponse: Adapter  = new Adapter");
            mAdapter = new QuestionPagerAdapter(this, mQuestionList);
            if (mPager != null) {
                mPager.setAdapter(mAdapter);
                mPager.setOffscreenPageLimit(0);


            }

        mTab.setTitles(mTitles);
        mTab.setTabIndex(0);
        mTab.setViewPager(mPager);
        //Hide the Progress Bar

        if (mBar.isShowing()){
            mBar.hide();
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return  mAdapter.getQuestionList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putParcelable(BundleKey.TEST_PAGE_SELECTION,); // TODO: 27/10/16 save adapter state
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {

        finishButtonClicked();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void TimeoutError() {
   // TODO: 10/11/16 set tag ,retry  , i.e add to Request quueue based on Request TAG

        FirebaseCrash.log("Time put Error ");
            mChangableRoot.removeAllViews();
            View v = View.inflate(this, R.layout.try_again_layout,mChangableRoot);


        }
    }

    @Override
    public void NetworkError() {


        mChangableRoot.removeAllViews();
        View v = View.inflate(this,R.layout.no_internet,mChangableRoot);

    }

    @Override
    public void ServerError() {
        mChangableRoot.removeAllViews();
        View v = View.inflate(this,R.layout.no_data,mChangableRoot);
    }

    @Override
    public void setQuestionList(List<Question> mQuestionList,String[] mTitles) {

    //set Adapter and page titles
        setAdapter(mTitles,mQuestionList);
    }

    @Override
    public void NoQuestionObtained() {

        mChangableRoot.removeAllViews();

        View view = View.inflate(this,R.layout.no_data,mChangableRoot);
    }

    @Override
    public void testSubmitted() {
        final Intent i = new Intent(this,TestCompletedActivity.class);
        if (mTestId!= null){

            i.putExtra(BundleKey.TEST_ID,mTestId);
            i.putExtra(BundleKey.TEST_DESC,mTestDesc);
            i.putExtra(BundleKey.SUBJECT_ID,mSubjectId);
        }
        startActivity(i);
    }

    @Override
    public void testNotSubmitted() {
        Toast.makeText(getApplicationContext(),"Test Not Submitted,Please Try again",Toast.LENGTH_SHORT).show();
        finish();
        Intent i = new Intent(this,TestWriteActivity.class);
        i.putExtra(BundleKey.TEST_ID,mTestId);
        i.putExtra(BundleKey.SUBJECT_ID,mSubjectId);
        startActivity(i);


    }
}
