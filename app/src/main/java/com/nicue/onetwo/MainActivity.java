package com.nicue.onetwo;

import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.nicue.onetwo.Utils.TimerBackend;
import com.nicue.onetwo.fragments.ChooserFragment;
import com.nicue.onetwo.fragments.CounterFragment;
import com.nicue.onetwo.fragments.DiceFragment;
import com.nicue.onetwo.fragments.StartFragment;
import com.nicue.onetwo.fragments.TimerFragment;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private Switch mSwitch;
    private Button rollAllButton;
    private Button resetButton;
    private Button addTimerButton;
    private Button delTimerButton;
    private TextView actionTitle;
    private NavigationView nvDrawer;
    private Fragment firstFragment;
    private boolean switchOn = false;
    private String currentTitleString;
    private ArrayList<TimerBackend> timerBackends = new ArrayList<>();

    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mSwitch = (Switch) toolbar.findViewById(R.id.toolbar_switch);
        resetButton = (Button) toolbar.findViewById(R.id.reset_chosen_button);
        rollAllButton = (Button) toolbar.findViewById(R.id.roll_dices_button);
        addTimerButton = (Button) toolbar.findViewById(R.id.add_timer_button);
        delTimerButton = (Button) toolbar.findViewById(R.id.remove_timer_button);
        actionTitle = (TextView) toolbar.findViewById(R.id.custom_toolbar_title);
        mSwitch.setOnCheckedChangeListener(this);
        resetButton.setOnClickListener(this);
        rollAllButton.setOnClickListener(this);
        addTimerButton.setOnClickListener(this);
        delTimerButton.setOnClickListener(this);
        setSupportActionBar(toolbar);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        setupDrawerContent(nvDrawer);


        // Find our drawer view
        drawerToggle = setupDrawerToggle();

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.addDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        if (getSupportFragmentManager().findFragmentById(R.id.m_content) == null){
            firstFragment = new CounterFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.m_content, firstFragment).commit();
            //actionTitle.setText(((CounterFragment) firstFragment).getTitle());
            currentTitleString = getString(R.string.menu_counter);
            actionTitle.setText(currentTitleString);

        }else{
            Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

            if (tempFragment instanceof CounterFragment){
                currentTitleString = getString(R.string.menu_counter);

            }else if (tempFragment instanceof DiceFragment){
                currentTitleString = getString(R.string.menu_dice);
                rollAllButton.setVisibility(View.VISIBLE);

            }else if (tempFragment instanceof ChooserFragment){
                currentTitleString = getString(R.string.menu_chooser);
                resetButton.setVisibility(View.VISIBLE);
                mSwitch.setVisibility(View.VISIBLE);

            }else if (tempFragment instanceof TimerFragment){
                currentTitleString = getString(R.string.menu_counter);
                delTimerButton.setVisibility(View.VISIBLE);
                addTimerButton.setVisibility(View.VISIBLE);

            }
            actionTitle.setText(currentTitleString);

        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int view_id = v.getId();
        switch (view_id){
            case R.id.roll_dices_button:
                rollAllDices();
                break;
            case R.id.add_timer_button:
                addTimer();
                break;
            case R.id.remove_timer_button:
                delTimer();
                break;
            case R.id.reset_chosen_button:
                resetChooser();
                break;

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof ChooserFragment){
            ((ChooserFragment) tempFragment).setChoosingOrder(isChecked);
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Let's check first if the fragment is the timer, to save its values
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof TimerFragment){
            timerBackends = ((TimerFragment) tempFragment).getData();
        }
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        switch(menuItem.getItemId()) {
            case R.id.nav_first_fragment:
                fragmentClass = CounterFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                resetButton.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                delTimerButton.setVisibility(View.INVISIBLE);
                addTimerButton.setVisibility(View.INVISIBLE);
                break;
            case R.id.nav_second_fragment:
                fragmentClass = DiceFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                resetButton.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.VISIBLE);
                delTimerButton.setVisibility(View.INVISIBLE);
                addTimerButton.setVisibility(View.INVISIBLE);
                break;
            case R.id.nav_third_fragment:
                fragmentClass = ChooserFragment.class;
                mSwitch.setVisibility(View.VISIBLE);
                resetButton.setVisibility(View.VISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                delTimerButton.setVisibility(View.INVISIBLE);
                addTimerButton.setVisibility(View.INVISIBLE);
                mSwitch.setChecked(false);
                break;
            case R.id.nav_fourth_fragment:
                fragmentClass = TimerFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                resetButton.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                delTimerButton.setVisibility(View.VISIBLE);
                addTimerButton.setVisibility(View.VISIBLE);
                break;
            default:
                fragmentClass = CounterFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();

            // now we fill the timerFragment if that's what we selected
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.m_content, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        currentTitleString = menuItem.getTitle().toString();
        actionTitle.setText(currentTitleString);
        // Close the navigation drawer
        mDrawer.closeDrawers();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteObj(View view){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof CounterFragment){
            ((CounterFragment) tempFragment).deleteObj(view);
        }
    }

    public void rollAllDices(){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof DiceFragment){
            ((DiceFragment) tempFragment).rollAllDices();
        }
    }

    public void addTimer(){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof TimerFragment){
            ((TimerFragment) tempFragment).addTimer();
        }
    }

    public void delTimer(){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof TimerFragment){
            ((TimerFragment) tempFragment).delTimer();
        }
    }

    public void resetChooser(){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof ChooserFragment){
            ((ChooserFragment) tempFragment).resetChooser();
        }
    }
    public void loadTimersData(){
        Fragment tempFragment2 = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment2 instanceof TimerFragment){
            if (timerBackends.size()>0) {
                ((TimerFragment) tempFragment2).setData(timerBackends);
            }
        }
        timerBackends.clear();
    }

    public void counterClicked(View v){
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = CounterFragment.class;
        mSwitch.setVisibility(View.INVISIBLE);
        rollAllButton.setVisibility(View.INVISIBLE);
        delTimerButton.setVisibility(View.INVISIBLE);
        addTimerButton.setVisibility(View.INVISIBLE);


        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.m_content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();


        // Highlight the selected item has been done by NavigationView
        nvDrawer.getMenu().getItem(0).setChecked(true);
        // Set action bar title
        currentTitleString = nvDrawer.getMenu().getItem(0).getTitle().toString();
        actionTitle.setText(currentTitleString);

    }
    public void timerClicked(View v){
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = TimerFragment.class;
        mSwitch.setVisibility(View.INVISIBLE);
        rollAllButton.setVisibility(View.INVISIBLE);
        delTimerButton.setVisibility(View.VISIBLE);
        addTimerButton.setVisibility(View.VISIBLE);

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.m_content, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        nvDrawer.getMenu().getItem(3).setChecked(true);
        // Set action bar title
        currentTitleString = nvDrawer.getMenu().getItem(0).getTitle().toString();
        actionTitle.setText(currentTitleString);

    }
    public void diceClicked(View v){
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = DiceFragment.class;
        mSwitch.setVisibility(View.INVISIBLE);
        rollAllButton.setVisibility(View.VISIBLE);
        delTimerButton.setVisibility(View.INVISIBLE);
        addTimerButton.setVisibility(View.INVISIBLE);
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.m_content, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        nvDrawer.getMenu().getItem(1).setChecked(true);
        // Set action bar title
        currentTitleString = nvDrawer.getMenu().getItem(0).getTitle().toString();
        actionTitle.setText(currentTitleString);

    }
    public void chooserClicked(View v){
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = ChooserFragment.class;
        mSwitch.setVisibility(View.VISIBLE);
        rollAllButton.setVisibility(View.INVISIBLE);
        delTimerButton.setVisibility(View.INVISIBLE);
        addTimerButton.setVisibility(View.INVISIBLE);
        mSwitch.setChecked(false);
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.m_content, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        nvDrawer.getMenu().getItem(2).setChecked(true);
        // Set action bar title
        currentTitleString = nvDrawer.getMenu().getItem(0).getTitle().toString();
        actionTitle.setText(currentTitleString);

    }

    /*
    public void rollDice(View view){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof DiceFragment){
            ((DiceFragment) tempFragment).rollDice(view);
        }
    }
    */

}


