package ifeoluwa.partscribber;

/*
Team Name - CPU
Project Name - Parts Crib Database
Member Names - Ifeoluwa David Adese, Mohand Ferawana, Tosin Ajayi
*/

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class PartsCribberActualHolders extends AppCompatActivity
{
    JSONObject jsonObject;
    JSONArray jsonArray;
    String selectedItem, selectedID, jsonstring;
    Intent intent;
    ListView listView;
    ActionBar actionBar;
    StudentHoldsAdapter studentHoldsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partscribber_actualholders);

        //Receive selected item from previous activity
        intent = getIntent();
        selectedItem = intent.getStringExtra("selectedItem");

        //Action Bar Configuration
        actionBar = getSupportActionBar();
        actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>"+selectedItem+"</font>"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Fetch list of students who still have the selected item in possession.
        new ItemInfoBackgroundTasks(this).execute();
    }

    class ItemInfoBackgroundTasks extends AsyncTask<Void, Void, String>
    {
        String json_url;
        String JSON_STRING;
        Context ctx;
        AlertDialog.Builder builder;
        private Activity activity;
        private AlertDialog loginDialog;

        public ItemInfoBackgroundTasks(Context ctx)
        {
            this.ctx = ctx;
            activity = (Activity)ctx;
        }

        @Override
        protected void onPreExecute()
        {
            builder = new AlertDialog.Builder(activity);
            View dialogView = LayoutInflater.from(this.ctx).inflate(R.layout.progress_dialog, null);
            ((TextView)dialogView.findViewById(R.id.tv_progress_dialog)).setText(getString(R.string.pleasewait));
            loginDialog = builder.setView(dialogView).setCancelable(false).show();
            json_url = getString(R.string.studentholds);
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                //Create Network Connection Request
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream os = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                String data = URLEncoder.encode("item_name", "UTF-8") + "=" + URLEncoder.encode(selectedItem, "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                os.close();
                InputStream is = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder = new StringBuilder();
                while ((JSON_STRING = bufferedReader.readLine()) != null)
                {
                    stringBuilder.append(JSON_STRING + "\n");
                }
                bufferedReader.close();
                is.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values)
        {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result)
        {
            loginDialog.dismiss();
            if(TextUtils.isEmpty(result)) //If JSON data returned is null i.e. Connection timed-out.
            {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(ctx);
                builder.setMessage(getString(R.string.connectionError));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        new ItemInfoBackgroundTasks(ctx).execute();
                    }
                });
                builder.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        finish();
                    }
                });
                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();
            }
            else
            {
                jsonstring = result;
                ArrayList<String> usernameList = new ArrayList<String>();
                ArrayList<String> quantityHeldList = new ArrayList<String>();
                ArrayList<HoldObjects> holdList = new ArrayList<HoldObjects>();
                final ArrayList<String> adapterCopy = new ArrayList<String>();

                try
                {
                    jsonObject = new JSONObject(jsonstring);
                    jsonArray = jsonObject.getJSONArray("server_response");
                    JSONObject JO = jsonArray.getJSONObject(0);

                    String username, quantityheld;

                    listView = (ListView) findViewById(R.id.listview);
                    studentHoldsAdapter = new StudentHoldsAdapter(ctx, R.layout.studentholds_rowlayout);
                    listView.setAdapter(studentHoldsAdapter);

                    username = JO.getString("allstudents");
                    quantityheld = JO.getString("quantityheld");

                    String[] usernameArray = username.split(",");
                    for(int i = 0; i < usernameArray.length; i++)
                    {
                        usernameList.add(usernameArray[i]);
                    }

                    String[] quantityArray = quantityheld.split(",");
                    for(int i = 0; i < quantityArray.length; i++)
                    {
                        quantityHeldList.add(quantityArray[i]);
                    }

                    for(int i = 0; i < usernameList.size(); i++)
                    {
                        if(Integer.valueOf(quantityHeldList.get(i)) > 0)
                        {
                            String theUsername = usernameList.get(i);
                            String theQuantityHeld = quantityHeldList.get(i);

                            HoldObjects user = new HoldObjects(theUsername.toUpperCase(), theQuantityHeld);
                            adapterCopy.add(theUsername.toUpperCase());
                            holdList.add(user);
                            studentHoldsAdapter.add(user);
                        }
                    }

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                    {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                        {
                            String selectedStudent = adapterCopy.get(position);
                            selectedID = selectedStudent;
                            new UserInfoBackgroundTasks(ctx).execute();
                        }
                    });
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    class UserInfoBackgroundTasks extends AsyncTask<String, Void, String>
    {
        Context ctx;
        String json_url, JSON_STRING;
        android.support.v7.app.AlertDialog.Builder builder, mBuilder;
        private Activity activity;
        private android.support.v7.app.AlertDialog loginDialog, dialog;
        View mView;
        TextView studentIDheader;
        String fname, lname, possessionqty, email, status;
        EditText studentfullname, studentpossessionqty, studentemail, studentstatus;

        public UserInfoBackgroundTasks(Context ctx)
        {
            this.ctx = ctx;
            activity = (Activity) ctx;
        }

        @Override
        protected void onPreExecute()
        {
            builder = new android.support.v7.app.AlertDialog.Builder(activity);
            View dialogView = LayoutInflater.from(this.ctx).inflate(R.layout.progress_dialog, null);
            ((TextView) dialogView.findViewById(R.id.tv_progress_dialog)).setText(getString(R.string.pleasewait));
            loginDialog = builder.setView(dialogView).setCancelable(false).show();
            json_url = getString(R.string.fetchStudentProfile);
        }

        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream os = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                String data = URLEncoder.encode(getString(R.string.username), "UTF-8") + "=" + URLEncoder.encode(selectedID, "UTF-8");

                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                os.close();
                InputStream is = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder = new StringBuilder();
                while ((JSON_STRING = bufferedReader.readLine()) != null)
                {
                    stringBuilder.append(JSON_STRING + "\n");
                }
                bufferedReader.close();
                is.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result)
        {
            loginDialog.dismiss();
            if(TextUtils.isEmpty(result))
            {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(ctx);
                builder.setMessage(getString(R.string.connectionError));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        new UserInfoBackgroundTasks(ctx).execute();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();
            }
            else
            {
                jsonstring = result;
                try
                {
                    jsonObject = new JSONObject(jsonstring);
                    jsonArray = jsonObject.getJSONArray("server_response");
                    JSONObject JO = jsonArray.getJSONObject(0);

                    fname = JO.getString("firstname");
                    lname = JO.getString("lastname");
                    possessionqty = JO.getString("possessionQty");
                    email = JO.getString("email");
                    status = JO.getString("status");

                    mBuilder = new android.support.v7.app.AlertDialog.Builder(activity);

                    LayoutInflater inflater = LayoutInflater.from(ctx);
                    mView = inflater.inflate(R.layout.studentinfo_alertdialog, null);

                    studentIDheader = (TextView) mView.findViewById(R.id.textView3);
                    studentfullname = (EditText) mView.findViewById(R.id.editText);
                    studentpossessionqty = (EditText) mView.findViewById(R.id.editText2);
                    studentemail = (EditText) mView.findViewById(R.id.editText3);
                    studentstatus = (EditText) mView.findViewById(R.id.editText4);

                    studentIDheader.setText(selectedID);
                    studentfullname.setText(getString(R.string.full_name) + ": " + fname + " " + lname);
                    studentpossessionqty.setText(getString(R.string.possession_qty) + ": " + possessionqty);
                    studentemail.setText(getString(R.string.email) + ": " + email);
                    studentstatus.setText(getString(R.string.status) + ": " + status);

                    mBuilder.setCancelable(false);
                    mBuilder.setView(mView);

                    dialog = mBuilder.create();
                    dialog.show();

                    final Button cancelBtn = (Button) mView.findViewById(R.id.button3);
                    cancelBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(final View v)
                        {
                            dialog.dismiss();
                        }
                    });

                    final Button rentalInfo = (Button) mView.findViewById(R.id.button2);
                    rentalInfo.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(final View v)
                        {
                            Intent intent = new Intent(ctx, PartsCribberReturnEquipment.class);
                            intent.putExtra("theID", selectedID);
                            startActivity(intent);
                        }
                    });
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public class HoldObjects
    {
        private String username, quantityHeld;

        public HoldObjects(String username, String quantityHeld)
        {
            this.username = username;
            this.quantityHeld = quantityHeld;
        }

        public String getUsername()
        {
            return username;
        }

        public String getQuantityHeld()
        {
            return quantityHeld;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        setIconInMenu(menu, R.id.about, R.string.about, R.mipmap.about);
        setIconInMenu(menu, R.id.help, R.string.help, R.mipmap.help);
        return super.onCreateOptionsMenu(menu);
    }

    private void setIconInMenu(Menu menu, int menuItemId, int labelId, int iconId)
    {
        MenuItem item = menu.findItem(menuItemId);
        SpannableStringBuilder builder = new SpannableStringBuilder("   " + getResources().getString(labelId));
        builder.setSpan(new ImageSpan(this, iconId), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        item.setTitle(builder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                break;

            case R.id.about:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.partscribdatabase.tech"));
                startActivity(browserIntent);
                break;

            case R.id.help:
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
                builder.setMessage("Mail: Prototypelab@humber.ca");
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
