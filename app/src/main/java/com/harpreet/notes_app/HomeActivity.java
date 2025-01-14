package com.harpreet.notes_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.harpreet.notes_app.Modal.Data;
import java.text.DateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private Toolbar mtoolbar;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;

    private FirebaseRecyclerAdapter<Data,Myviewholder> adapter;    //These lines declare various variables used within the HomeActivity class, including references to UI
                                                                    // elements (toolbar, floating action button, and recycler view), an adapter for
                                                                    // the recycler view, instances of Firebase-related classes (FirebaseAuth and DatabaseReference),
                                                                    // and other variables to store data.
    private FirebaseAuth mauth;
    private DatabaseReference mDatabase;

    public String name;
    public String description;
    public String post_key;
    private ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        pd = new ProgressDialog(this);
        pd.setMessage("Fetching Data...");
        pd.show();
        mtoolbar = findViewById(R.id.tbar);
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);


        setSupportActionBar(mtoolbar);
        getSupportActionBar().setTitle("Keep My Notes");
        mauth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mauth.getCurrentUser();
        String uid = mUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("All data").child(uid);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addData();
            }
        });


    }
    private void addData()             //This method is responsible for displaying an AlertDialog that allows the user to input data (name and description) for a new note.
    {
        AlertDialog.Builder mydialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View myview = inflater.inflate(R.layout.input_layout,null);
        mydialog.setView(myview);

        final AlertDialog myalert_dialogue = mydialog.create();
        myalert_dialogue.setCancelable(false);
        myalert_dialogue.show();

        final EditText name = myview.findViewById(R.id.name);
        final EditText des = myview.findViewById(R.id.des);

        Button btnCancle = myview.findViewById(R.id.cancle);
        Button btnsave = myview.findViewById(R.id.save);

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mname = name.getText().toString().trim();
                String mdes = des.getText().toString().trim();
                if (TextUtils.isEmpty(mname))
                {
                    name.setError("Required Field");
                    return;
                }
                if (TextUtils.isEmpty(mdes))
                {
                    des.setError("Required Field");
                    return;
                }
                String id = mDatabase.push().getKey();
                String mDate = DateFormat.getDateInstance().format(new Date());

                Data data = new Data(mname,mdes,id,mDate);
                mDatabase.child(id).setValue(data);

                myalert_dialogue.dismiss();
            }
        });

        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myalert_dialogue.dismiss();
            }
        });

    }


    @Override
    protected void onStart() {  //It sets up the FirebaseRecyclerAdapter for displaying the notes in the RecyclerView and starts listening for changes in the data.
        super.onStart();

        Query query = FirebaseDatabase.getInstance().getReference().child("All data").child(mauth.getUid());
        FirebaseRecyclerOptions<Data> options = new FirebaseRecyclerOptions.Builder<Data>().setQuery(query,new SnapshotParser<Data>(){
            @NonNull
            @Override
            public Data parseSnapshot(@NonNull DataSnapshot snapshot) {

                return new Data(snapshot.child("name").getValue().toString(),snapshot.child("description").getValue().toString(),snapshot.child("id").getValue().toString(),
                        snapshot.child("date").getValue().toString());
            } }).build();

        adapter = new FirebaseRecyclerAdapter<Data, Myviewholder>(options) {
            @NonNull
            @Override
            public Myviewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_layout_design,viewGroup,false);
                return new Myviewholder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull Myviewholder holder, final int position, @NonNull final Data model) {
                pd.dismiss();
                holder.setDate(model.getDate());
                holder.setDescription(model.getDescription());
                holder.setName(model.getName());
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(HomeActivity.this, "Item Selected", Toast.LENGTH_SHORT).show();
                        post_key = getRef(position).getKey();
                        name = model.getName();
                        description = model.getDescription();
                        updateData();
                    }
                });
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();


    }

    @Override
    protected void onStop() {  //This method is called when the activity is stopped. It stops the FirebaseRecyclerAdapter from listening to changes in the data.
        super.onStop();
        adapter.stopListening();

    }

    public static  class Myviewholder extends RecyclerView.ViewHolder{          //This is a static inner class that defines a
                                                                // custom RecyclerView.ViewHolder for the notes. It holds references
                                                                 //   to the UI elements within each item of the RecyclerView.

        View mView;

        public Myviewholder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }
        public void setName(String name){
            TextView Mname = mView.findViewById(R.id.name_item);
            Mname.setText(name);
        }
        public void setDescription(String description)
        {
            TextView Mdescription = mView.findViewById(R.id.des_item);
            Mdescription.setText(description);
        }
        public void setDate(String date)
        {
            TextView Mdate = mView.findViewById(R.id.date);
            Mdate.setText(date);
        }


    }

    public void updateData() //
   // The updateData() method is responsible for displaying an AlertDialog that allows the user to update the
    // selected note's data or delete the note. Here's a breakdown of what the method does:

    {

        final AlertDialog.Builder mydia = new AlertDialog.Builder(this);
        LayoutInflater inflater  = LayoutInflater.from(this);
        View myview = inflater.inflate(R.layout.update_layout,null);
        mydia.setView(myview);
        final AlertDialog alertDialog = mydia.create();

        alertDialog.show();
        final EditText mName = myview.findViewById(R.id.name);
        final EditText mDescription = myview.findViewById(R.id.des);
        mName.setText(name);
        mName.setSelection(name.length());

        mDescription.setText(description);
        mDescription.setSelection(description.length());

        Button btndelete = myview.findViewById(R.id.btndelete);
        Button btnupdate = myview.findViewById(R.id.btnupdate);
        btndelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child(post_key).removeValue();
                alertDialog.dismiss();

            }
        });
        btnupdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = mName.getText().toString().trim();
                description = mDescription.getText().toString();
                if (TextUtils.isEmpty(name))
                {
                    mName.setError("Required Field");
                    return;
                }
                if (TextUtils.isEmpty(description))
                {
                    mDescription.setError("Required Field");
                    return;
                }


                String mDate =DateFormat.getDateInstance().format(new Date());
                Data data = new Data(name,description,post_key,mDate);
                mDatabase.child(post_key).setValue(data);
                alertDialog.dismiss();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.logout:
                mauth.signOut();
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
