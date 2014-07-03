package com.holandago.wbamanager.ordersmanager;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.holandago.wbamanager.R;
import com.holandago.wbamanager.library.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class OperationsList extends ActionBarActivity {
    private static String targetUrl = "http://wba-urbbox-teste.herokuapp.com/rest/operations";
    public final static String OPERATIONS_MESSAGE =
            "com.holandago.wbamanager.ordersmanager.OPERATIONS_MESSAGE";
    public final static String ORDER_TITLE_MESSAGE =
            "com.holandago.wbamanager.ordersmanager.ORDER_TITLE_MESSAGE";
    public final static String LOT_NUMBER_MESSAGE =
            "com.holandago.wbamanager.ordersmanager.LOT_NUMBER_MESSAGE";
    public final static String ORDER_ID_MESSAGE =
            "com.holandago.wbamanager.ordersmanager.ORDER_ID_MESSAGE";
    public final static String OPERATION_MESSAGE =
            "com.holandago.wbamanager.ordersmanager.OPERATION_MESSAGE";
    private ListView listView;
    private static final String PROGRESS_ID_TAG = "progress_id";
    private static final String WBA_ORANGE_COLOR = "#FBB03B";
    private static final String NEXT_OPERATION_TAG = "next_operation";
    private static final String MACHINE_TAG = "machine";
    private static final String OWNER_ID_TAG = "owner_id";
    private static final String OWNER_NAME_TAG = "owner_name";
    private static final String STATUS_TAG = "status";
    private static final String OPERATION_NUMBER_TAG = "no";
    private static final String START_TIME_TAG = "start_time";
    private static final String CUSTOMER_TAG = "costumer";
    private static final String PART_TAG = "part";
    private static final String PROJECT_NAME_TAG = "title";
    private static final String TIME_TAG = "time";
    private static final String ORDER_ID_TAG = "order_id";
    private static final String ID_TAG = "id";
    private static final String OPERATION_NAME_TAG = "operation_name";
    private static final String LOT_NUMBER_TAG = "lot";
    private String operations;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        session = new SessionManager(getApplicationContext());
        if(session.isLoggedIn())
            new JSONParse(session.getUserDetails()
                    .get(SessionManager.KEY_ID)).execute();
        else{
            startLoginActivity();
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //Inflating the actionBar menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.order_list_actions,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
                new JSONParse(session.getUserDetails()
                        .get(SessionManager.KEY_ID)).execute();
                return true;
            case R.id.action_qrcode:
                try{
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE","QR_CODE_MODE");
                    startActivityForResult(intent,0);
                }catch(Exception e){
                    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                    startActivity(marketIntent);
                    e.printStackTrace();
                }
                return true;
            case R.id.action_logout:
                session.logoutUser();
                startLoginActivity();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                String orderUrl = data.getStringExtra("SCAN_RESULT");
                try {
                    ArrayList<HashMap<String,String>> operationList =
                            UserOperations.getOperationsList();
                    JSONObject json = new JSONObject(orderUrl);
                    String id = json.getString(ORDER_ID_TAG);
                    Log.e("DEBUGGING", "ORDER ID = "+id);
                    String lot = json.getString(LOT_NUMBER_TAG);
                    ArrayList<HashMap<String,String>> operationsFromOrder =
                            new ArrayList<HashMap<String, String>>();
                    for(HashMap<String,String> operation : operationList){
                        if(operation.get(ORDER_ID_TAG).equals(id)){
                            if(operation.get(LOT_NUMBER_TAG).equals(lot)){
                                operationsFromOrder.add(operation);
                            }
                        }
                    }
                    Collections.sort(operationsFromOrder,new NumberComparator());
                    sendOperationMessage(
                            //Sends first element of the ordered Array
                            operationsFromOrder.get(0));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else
            if(resultCode == RESULT_CANCELED){
                onResume();
            }
        }else
        if(requestCode==1){
            //From an DisplayOperationActivity
            updateOrCreateList();
        }else
        if(requestCode == 2){
            //From an LoginActivity
            if(resultCode == RESULT_OK){
                new JSONParse(session.getUserDetails()
                        .get(SessionManager.KEY_ID)).execute();
            }
        }
    }

    private void startLoginActivity(){
        ArrayList<HashMap<String,String>> operations = UserOperations.getOperationsList();
        operations = null;
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 2);
    }


    public void sendOperationMessage(HashMap<String,String> operation){
        //Travels to DisplayLotsActivity
        Intent intent = new Intent(this, DisplayOperationActivity.class);
        JSONObject operationJson = new JSONObject(operation);
        intent.putExtra(OPERATION_MESSAGE,operationJson.toString());
        startActivityForResult(intent, 1);
    }

    public void updateOrCreateList(){
        //filling the next operations:
        ArrayList<HashMap<String,String>> operationList = UserOperations.getOperationsList();
        //Ordering by lot
        Collections.sort(operationList, new LotNumberComparator());
        //Getting the next operation
        for(HashMap<String,String> map1 :operationList){
            for(HashMap<String,String> map2:operationList){
                if(!map2.equals(map1)){
                       //Equal lot
                    if(map2.get(LOT_NUMBER_TAG).equals(map1.get(LOT_NUMBER_TAG)) &&
                       //Not finished
                       !map2.get(STATUS_TAG).equals("2")){
                        map1.put(NEXT_OPERATION_TAG,map2.get(OPERATION_NAME_TAG));
                        break;
                    }
                }
            }
        }
        //Will make sure that we only see the first operation of each id
        HashMap<String,HashMap<String,String>> uniqueID =
                new HashMap<String, HashMap<String, String>>();
        //Only one for each ID and only the unfinished ones
        for(int i = operationList.size()-1;i>=0;i--){
            if(!operationList.get(i).get(STATUS_TAG).equals("2"))
                uniqueID.put(operationList.get(i).get(ID_TAG),operationList.get(i));
        }
        //Holds the value of the uniqueOperations that are unfinished
        ArrayList<HashMap<String,String>> uniqueOperations =
                new ArrayList<HashMap<String, String>>(uniqueID.values());
        Collections.sort(uniqueOperations,new LotNumberComparator());
        //Needs to be a final because it's called from an inner class
        final ArrayList<HashMap<String,String>> uniqueOperationsf =
                new ArrayList<HashMap<String, String>>(uniqueID.values());
        listView = (ListView)findViewById(R.id.orderList);
        ListAdapter adapter = new SimpleAdapter(
                OperationsList.this, //Context
                uniqueOperationsf, //Data
                R.layout.operations_list_v, //Layout
                new String[]{PART_TAG, MACHINE_TAG, TIME_TAG,
                LOT_NUMBER_TAG}, //from
                new int[]{R.id.operation_list_name, R.id.operation_list_machine,
                        R.id.operation_list_time, R.id.operation_list_lot} //to
        );
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //Sends the operations part of the JSONObject to the next activity
                sendOperationMessage(uniqueOperationsf.get(+position));
            }
        });
    }

    public class LotNumberComparator implements Comparator<HashMap<String,String>> {
        @Override
        public int compare(HashMap<String,String> operation1, HashMap<String,String> operation2){
            return operation1.get(LOT_NUMBER_TAG).compareTo(operation2.get(LOT_NUMBER_TAG));
        }
    }

    public class NumberComparator implements Comparator<HashMap<String,String>>{
        @Override
        public int compare(HashMap<String,String> operation1, HashMap<String,String>operation2){
            return
              operation1.get(OPERATION_NUMBER_TAG).compareTo(operation2.get(OPERATION_NUMBER_TAG));
        }
    }

    public void createList(){
        ArrayList<HashMap<String,String>> operationList = UserOperations.getOperationsList();
        try{
            //Gets the operation from a JSONArray and put them in an ArrayList of Hashmaps with
            //the key value pairs.
            JSONArray json = new JSONArray(operations);
            for(int i = 0; i< json.length(); i++){
                JSONObject object = json.getJSONObject(i);
                //String projectName = object.getString(PROJECT_NAME_TAG);
                String part = object.getString(PART_TAG);
                String opNo = object.getString(OPERATION_NUMBER_TAG);
                String pID = object.getString(PROGRESS_ID_TAG);
                String customer = object.getString(CUSTOMER_TAG);
                String ownerId = object.getString(OWNER_ID_TAG);
                String machine = object.getString(MACHINE_TAG);
                String status = object.getString(STATUS_TAG);
                String orderID = object.getString(ORDER_ID_TAG);
                String time = object.getString(TIME_TAG);
                String lot = object.getString(LOT_NUMBER_TAG);
                String operation_name = object.getString(OPERATION_NAME_TAG);
                String id = object.getString(ID_TAG);
                HashMap<String,String> map = new HashMap<String, String>();
                //map.put(PROJECT_NAME_TAG,projectName);
                map.put(PART_TAG,part);
                map.put(PROGRESS_ID_TAG,pID);
                map.put(OPERATION_NUMBER_TAG,opNo);
                map.put(ORDER_ID_TAG,orderID);
                map.put(CUSTOMER_TAG,customer);
                map.put(MACHINE_TAG,machine);
                map.put(OWNER_ID_TAG,ownerId);
                map.put(LOT_NUMBER_TAG,lot);
                map.put(STATUS_TAG,status);
                map.put(TIME_TAG,time);
                map.put(OPERATION_NAME_TAG,operation_name);
                map.put(ID_TAG,id);
                map.put(OWNER_NAME_TAG,session.getUserDetails().get(SessionManager.KEY_USER));
                //Assuming the title is the ID
                operationList.add(map);
                updateOrCreateList();
            }
        }catch(JSONException e){
            e.printStackTrace();
            Log.e("JSON Error", "JSON error on create list " + e.toString());
        }finally{
            //If for some reason he couldn't get the elements:
            if(operationList.isEmpty())
                cannotGetOperations();
        }
    }

    public void cannotGetOperations(){
        new AlertDialog.Builder(this)
                .setTitle("Cannot get operations from server")
                .setNeutralButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new JSONParse(session.getUserDetails()
                                .get(SessionManager.KEY_ID))
                                .execute();
                    }
                }).create().show();
    }


    public class JSONParse extends AsyncTask<String,String, JSONArray>{
        private ProgressDialog pDialog;
        private String userID;
        private JSONParser parser = new JSONParser();

        public JSONParse(String userID){
            this.userID = userID;
        }

        @Override
        protected JSONArray doInBackground(String... strings) {
            return parser.getJSONfromUrl(targetUrl+"/"+userID);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            pDialog = new ProgressDialog(OperationsList.this);
            pDialog.setMessage("Getting data...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(JSONArray json){
            pDialog.dismiss();
            try {
                operations = json.toString();
                createList();
            }catch (NullPointerException e){
                cannotGetOperations();
            }
        }

    }

    private class CustomAdapter extends BaseAdapter{
        private LayoutInflater inflater;
        private ArrayList<HashMap<String,String>> data;

        public CustomAdapter(Context context, ArrayList<HashMap<String,String>> data){
            this.inflater = LayoutInflater.from(context);
            this.data = data;
        }

        public int getCount(){
            return data.size();
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public HashMap<String,String> getItem(int position){
            return data.get(+position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            OperationViewHolder holder;
            if(convertView == null){
                convertView = inflater.inflate(R.layout.operations_list_v,null);
                holder = new OperationViewHolder();
                holder.operation_name = (TextView)convertView.findViewById(R.id.operation_list_name);
                holder.operation_machine =
                        (TextView)convertView.findViewById(R.id.operation_list_machine);
                holder.operation_lot = (TextView)convertView.findViewById(R.id.operation_list_lot);
                holder.operation_time = (TextView)convertView.findViewById(R.id.operation_list_time);
                holder.operation_project = (TextView)convertView.findViewById(R.id.operation_list_project);
                holder.background = (LinearLayout)convertView.findViewById(R.id.operation_list_background);
                convertView.setTag(holder);
            }else{
                holder = (OperationViewHolder)convertView.getTag();
            }

            holder.operation_name.setText(data.get(+position).get(OPERATION_NAME_TAG));
            holder.operation_machine.setText(data.get(+position).get(MACHINE_TAG));
            holder.operation_lot.setText(data.get(+position).get(LOT_NUMBER_TAG));
            holder.operation_time.setText("Time: "+data.get(+position).get(TIME_TAG));

            if(getItem(position).get(STATUS_TAG).equals("1")){
                holder.background.setBackgroundColor(Color.parseColor(WBA_ORANGE_COLOR));
            }

            return convertView;
        }

    }

    private static class OperationViewHolder {
        TextView operation_name;
        TextView operation_machine;
        TextView operation_lot;
        TextView operation_time;
        TextView operation_project;
        LinearLayout background;
    }

}