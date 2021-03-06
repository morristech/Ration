package com.himanshurawat.ration.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Toast;

import com.himanshurawat.ration.PersonActivity;
import com.himanshurawat.ration.R;
import com.himanshurawat.ration.adapter.PeopleAdapter;
import com.himanshurawat.ration.respository.db.entity.People;
import com.himanshurawat.ration.respository.network.NetworkClient;
import com.himanshurawat.ration.util.Constant;
import com.himanshurawat.ration.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;

public class UserActivity extends AppCompatActivity implements PeopleAdapter.OnPeopleSelectedListener {

    private RecyclerView peopleRecyclerView;
    private SharedPreferences userPref;
    private PeopleAdapter peopleAdapter;
    private List<People> peopleList;
    private LinearLayoutManager linearLayoutManager;

    private Retrofit retrofit;

    private String token;

    private UserViewModel viewModel;

    private boolean isScrolling;

    private int currentItems, totalItems, scrolledOutItems;

    private int page = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        peopleRecyclerView = findViewById(R.id.activity_user_recycler_view);
        peopleList = new ArrayList<>();
        peopleAdapter = new PeopleAdapter(this,peopleList,this);
        peopleRecyclerView.setAdapter(peopleAdapter);
        linearLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        peopleRecyclerView.setLayoutManager(linearLayoutManager);

        retrofit = NetworkClient.getNetworkClient();

        userPref = getApplicationContext().getSharedPreferences(Constant.USER_PREF,MODE_PRIVATE);

        viewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        token = userPref.getString(Constant.SESSION_KEY,null);
        if(token == null){
            invalidateSession(this);
        }

        viewModel.isSessionValid.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && !aBoolean){
                    userPref.edit().putString(Constant.SESSION_KEY,null).apply();
                    invalidateSession(UserActivity.this);
                }
            }
        });

        viewModel.getData().observe(this, new Observer<List<People>>() {
            @Override
            public void onChanged(@Nullable List<People> people) {
                if(people != null && people.size()<1){
                    viewModel.fetchFromInternet(token,page);
                }else{
                    if(people != null) {
                        int startingIndex = peopleList.size();
                        int endingIndex = people.size()-1;
                        List<People> subList = people.subList(startingIndex,endingIndex);
                        peopleList.addAll(subList);
                        peopleAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        peopleRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                currentItems = linearLayoutManager.getChildCount();
                totalItems = linearLayoutManager.getItemCount();
                scrolledOutItems = linearLayoutManager.findFirstVisibleItemPosition();

                if(isConnected() && isScrolling && (currentItems + scrolledOutItems ==  totalItems)){
                    viewModel.fetchFromInternet(token,++page);
                }
            }
        });



//        NetworkService networkService = retrofit.create(NetworkService.class);
//        networkService.getPeopleFromNetWork(token,1).enqueue(new Callback<PeopleResponse>() {
//            @Override
//            public void onResponse(Call<PeopleResponse> call, Response<PeopleResponse> response) {
//
//                if(response.body() !=null){
//                    if(response.body().getStatus().equals(Constant.RESPONSE_SUCCESS)){
//                        peopleList.addAll(response.body().getPeople());
//                        peopleAdapter.notifyDataSetChanged();
//                    }else if(response.body().getStatus().equals(Constant.RESPONSE_FAILURE)) {
//                        Toast.makeText(UserActivity.this, response.body().getError(), Toast.LENGTH_LONG).show();
//                        token = null;
//                        userPref.edit().putString(Constant.SESSION_KEY,null).apply();
//                        startActivity(new Intent(UserActivity.this,LoginActivity.class));
//                    }
//                }
//
//            }
//
//            @Override
//            public void onFailure(Call<PeopleResponse> call, Throwable t) {
//                Toast.makeText(UserActivity.this, t.getMessage() +"", Toast.LENGTH_SHORT).show();
//            }
//        });




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user,menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.meanu_user_search_icon){
            startActivity(new Intent(this,SearchActivity.class));
        }

        if(item.getItemId() == R.id.menu_user_logout_icon){
            invalidateSession(this);
        }

        return true;
    }

    @Override
    public void onPeopleSelected(int position) {
        Intent intent = new Intent(UserActivity.this,PersonActivity.class);
        intent.putExtra(Constant.PERSON_KEY,peopleList.get(position));
        startActivity(intent);
    }

    private void invalidateSession(Context context){
        //Toast.makeText(this,"Session has Expired",Toast.LENGTH_LONG).show();
        SharedPreferences userPref = context.getApplicationContext().getSharedPreferences(Constant.USER_PREF,MODE_PRIVATE);
        userPref.edit().putString(Constant.SESSION_KEY,null).apply();
        Intent intent = new Intent(context,LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    private boolean isConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().isConnected();
    }

}
