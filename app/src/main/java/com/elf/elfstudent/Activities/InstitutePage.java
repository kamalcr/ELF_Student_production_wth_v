package com.elf.elfstudent.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.elf.elfstudent.Adapters.ClassSpinnerAdapter;
import com.elf.elfstudent.Adapters.SchoolListAdapter;
import com.elf.elfstudent.DataStorage.DataStore;
import com.elf.elfstudent.Network.AppRequestQueue;
import com.elf.elfstudent.Network.ErrorHandler;
import com.elf.elfstudent.Network.JsonProcessors.InstituteRespHandler;
import com.elf.elfstudent.Network.JsonProcessors.RegisterListener;
import com.elf.elfstudent.R;
import com.elf.elfstudent.Utils.BundleKey;
import com.elf.elfstudent.Utils.RequestParameterKey;
import com.elf.elfstudent.Utils.ScreenUtil;
import com.elf.elfstudent.model.InstitutionModel;
import com.elf.elfstudent.model.StandardModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nandhu on 20/10/16.
 * The Instituite and Standard Choosing Page
 */

public class InstitutePage extends AppCompatActivity implements
        ErrorHandler.ErrorHandlerCallbacks, InstituteRespHandler.InstituteHandler, RegisterListener.RegistrationCallback {


    private static final String TAG = "ELF";
    private static final String GET_INSTITUTE_URL = "http://elfanalysis.net/elf_ws.svc/GetInstitutionList";
    private static final String REGISTER_URL = "http://elfanalysis.net/elf_ws.svc/StudentRegistration";
    String boardId  = null;
    String stateId = null;


    //The Views

    @BindView(R.id.institute_page_register_button)
    Button mRegisterButton;


    @BindView(R.id.ins_autocomplete)
    Spinner mInsTextView;

    //standard Spinner can be 10th or 11th or 12th
    @BindView(R.id.ins_std_spinner) Spinner mSpinner;

    @BindView(R.id.ins_imageview)
    ImageView mBacImage;



    //The Toolabar


    List<InstitutionModel> institutionList;

    private AppRequestQueue mRequestQueue = null;
    ErrorHandler errorHandler = null;
    InstituteRespHandler instituteRespHandler = null;

    DataStore mStore = null;
    RegisterListener mRegisterListener = null;

    SchoolListAdapter mSchoolAdapter = null;
    private String ins_id = null;
    List<StandardModel> classList = null;

    ClassSpinnerAdapter mClassAdapter = null;
    String classid = null;

    JsonArrayRequest mInsRequest = null;
    JsonArrayRequest mRegisterRequest = null;


    String ins_name  = null;
    ProgressDialog mDialog = null;
    boolean isButtonPressed = false;
    private String groupId = null;
    private ProgressDialog mRegisterDialog = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.institute_page);
        ButterKnife.bind(this);

//        get Board Id and State Id and Set it to Adapter
//        if (getIntent() != null){
//            boardId = getIntent().getStringExtra(BundleKey.ARG_BOARD_ID);
//            stateId  = getIntent().getStringExtra(BundleKey.ARG_STATE_ID);

//        }
        //get a Handle to Saved Values


        mDialog = new ProgressDialog(this);
        mDialog.setIndeterminate(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setMessage("Getting School List..");
        mDialog.show();

        mStore  = DataStore.getStorageInstance(getApplicationContext());

        //The Network Handler objects
        errorHandler = new ErrorHandler(this);
        instituteRespHandler = new InstituteRespHandler(this);
        mRegisterListener = new RegisterListener(this);

        mRequestQueue  = AppRequestQueue.getInstance(getApplicationContext());

        prepareAdapterForInstituion("1","1");

        //Populate Class List
        classList = new ArrayList<>(2);
        classList.add(new StandardModel("10 Standard","10"));
        classList.add(new StandardModel("12th Standard","12"));

        //prepare Adapter for Class SPinner
        mClassAdapter = new ClassSpinnerAdapter(getApplicationContext(),R.layout.simple_spinner,classList);

        mSpinner.setAdapter(mClassAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                Log.d(TAG, "onItemSelected: standar "+classList.get(i).getClassID());
                classid = classList.get(i).getClassID();
                mStore.setStudentStandard(classid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });




        //Register Button
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Find which group
                pickGroupId();

            }
        });


        //The autcomplete Textview

        mInsTextView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                InstitutionModel ins = institutionList.get(i);
                ins_id = ins.getIns_id();
                ins_name = ins.getInsName();
                Log.d(TAG, "onItemSelected: "+ins_id);
                mStore.setInstitutionId(ins_id);
                mStore.setInstituionName(ins_name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    private void pickGroupId() {
        if (classid == null){
            Toast.makeText(this,"Select Grade",Toast.LENGTH_SHORT).show();

        }
        else{
            //Class id has been selected
            if (classid.equals("10")){
                groupId = "0";
                RegisterStudent(groupId);
            }
            else{
                //Class Id 12 find , ask WHich group
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                LayoutInflater inflater = this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.group_picker, null);
                alertDialog.setView(dialogView);
                ImageButton mCsButton  = (ImageButton) dialogView.findViewById(R.id.cs_image);
                ImageButton bioButton = (ImageButton) dialogView.findViewById(R.id.bio_button);
                mCsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: 15/11/16 find computer id
                        groupId = "1";
                        RegisterStudent(groupId);
                    }
                });

                bioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        groupId = "2";
                        RegisterStudent(groupId);
                    }
                });


                alertDialog.setCancelable(false);
                alertDialog.show();


            }
        }
    }

    private void RegisterStudent(String groupId) {
        JSONObject mObject = new JSONObject();
        mRegisterDialog = new ProgressDialog(this);
        mRegisterDialog.setMessage("Registering. Please wait..");
        mRegisterDialog.setCanceledOnTouchOutside(false);
        mRegisterDialog.setIndeterminate(true);
        mRegisterDialog.show();







        //Request Body Objects
        try{


            if (mStore!=null){




                mObject.put("FirstName",mStore.getUserName());
                mObject.put(RequestParameterKey.LOGIN_LAS_NAME,"null");
                mObject.put(RequestParameterKey.EMAIL_ID,mStore.getEmailId());
                mObject.put(RequestParameterKey.PASSWORD,mStore.getPassWord());
                mObject.put(RequestParameterKey.PHONE,mStore.getPhoneNumber());
                mObject.put(RequestParameterKey.INSTITUION_ID,ins_id);
                mObject.put(RequestParameterKey.board_id,"1");
                if (classid.equals("10")){

                    mObject.put(RequestParameterKey.CLASS_ID,"1");
                }
                else{
                    mObject.put(RequestParameterKey.CLASS_ID,"2");
                }
                mObject.put(RequestParameterKey.CITY_ID,"1");
                mObject.put(RequestParameterKey.DISTRICT_ID,"1");
                mObject.put(RequestParameterKey.STATE_ID,"1");


//
                mObject.put(RequestParameterKey.GROUP_ID, groupId);

                Log.d(TAG, "Registering Student "+mObject.toString());
                //get Instituion Id
            }
            else{
                throw new NullPointerException("Store is null");
            }
//            mObject.put(RequestParameterKey.INSTITUION_ID,mStore.)

            //get standard
//            mObject.put(RequestParameterKey.STANDARD,)
        }
        catch (Exception e ){
            Log.d(TAG, "RegisterStudent: "+e.getLocalizedMessage());
        }

        //Request Body
        JsonArrayRequest mRequest = new JsonArrayRequest(Request.Method.POST,REGISTER_URL,mObject,mRegisterListener,errorHandler);
        isButtonPressed = true;

        //add to request Queue
        mRequestQueue.addToRequestQue(mRequest);
    }

    private void prepareAdapterForInstituion(String boardId, String stateId) {

        JSONObject mObject = new JSONObject();
        try{
            mObject.put(RequestParameterKey.BOARD_ID,"1");
            mObject.put(RequestParameterKey.STATE_ID,"1");
        }
        catch (Exception e ){
            Log.d(TAG, "prepareAdapterForInstituion: ");

        }
       mInsRequest  = new JsonArrayRequest(Request.Method.POST
                ,GET_INSTITUTE_URL,mObject
                ,instituteRespHandler,errorHandler);
        mRequestQueue.addToRequestQue(mInsRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void TimeoutError() {


       Toast.makeText(getApplicationContext(),"Try again or Make sure You have Internet",Toast.LENGTH_SHORT).show();
        

    }

    @Override
    public void NetworkError() {
      Toast.makeText(this,"Please connect to Internet",Toast.LENGTH_LONG).show();
    }

    @Override
    public void ServerError() {
        Log.d(TAG, "ServerError: ");
    }

    /*This Method gives us the list of institution from { @link InstituteRespHandler }
    * Prepare the Adatpter and set it to Cusotm Autcomplete
    *
    *
    *
    *
    * */

    @Override
    public void setInstitutionList(List<InstitutionModel> list) {
        Log.d(TAG, "setInstitutionList: ");
        stopDialog();
        this.institutionList = list;
        mSchoolAdapter = new SchoolListAdapter(this,R.layout.institution_item_row_new,list);
        mInsTextView.setAdapter(mSchoolAdapter);




    }

    private void stopDialog() {
        if (mDialog != null && mDialog.isShowing()){

            mDialog.dismiss();
        }
    }


    @Override
    public void Registered(String studentId) {

        if(mRegisterDialog != null && mRegisterDialog.isShowing()){
            mRegisterDialog.dismiss();
        }
        if (mStore != null){
            Log.d(TAG, "Registered: student Id "+studentId);
            //set studentId
            mStore.setStudentId(studentId);
            mStore.setStudentPrefrerredSubject(groupId);
            mStore.setIsFirstTime(false);


        }
        else{
            throw new NullPointerException("Store value cannot be null");
        }


        //Show Home Activity
            final  Intent i = new Intent(this,HomeActivity.class);
            startActivity(i);

    }

    @Override
    public void NotRegistered() {
        if(mRegisterDialog != null && mRegisterDialog.isShowing()){
            mRegisterDialog.dismiss();
        }
        Toast.makeText(this,"Not Registered Please try again",Toast.LENGTH_SHORT).show();
    }


}
