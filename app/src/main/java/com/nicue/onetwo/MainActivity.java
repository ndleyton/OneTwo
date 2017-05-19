package com.nicue.onetwo;

import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.nicue.onetwo.fragments.ChooserFragment;
import com.nicue.onetwo.fragments.CounterFragment;
import com.nicue.onetwo.fragments.DiceFragment;
import com.nicue.onetwo.fragments.TimerFragment;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private Switch mSwitch;
    private Button rollAllButton;
    private Button addTimerButton;
    private TextView actionTitle;
    private NavigationView nvDrawer;
    private Fragment firstFragment;
    private boolean switchOn = false;
    private String currentTitleString;

    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mSwitch = (Switch) toolbar.findViewById(R.id.toolbar_switch);
        rollAllButton = (Button) toolbar.findViewById(R.id.roll_dices_button);
        addTimerButton = (Button) toolbar.findViewById(R.id.add_timer_button);
        actionTitle = (TextView) toolbar.findViewById(R.id.custom_toolbar_title);
        mSwitch.setOnCheckedChangeListener(this);
        rollAllButton.setOnClickListener(this);
        addTimerButton.setOnClickListener(this);
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
            currentTitleString = "Counter";
            actionTitle.setText(currentTitleString);
        }else{
            Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

            if (tempFragment instanceof CounterFragment){
                currentTitleString = "Counter";

            }else if (tempFragment instanceof DiceFragment){
                currentTitleString = "Dice";
                rollAllButton.setVisibility(View.VISIBLE);

            }else if (tempFragment instanceof ChooserFragment){
                currentTitleString = "Chooser";
                mSwitch.setVisibility(View.VISIBLE);

            }else if (tempFragment instanceof TimerFragment){
                currentTitleString = "Timer";

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
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        switch(menuItem.getItemId()) {
            case R.id.nav_first_fragment:
                fragmentClass = CounterFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                break;
            case R.id.nav_second_fragment:
                fragmentClass = DiceFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.VISIBLE);
                break;
            case R.id.nav_third_fragment:
                fragmentClass = ChooserFragment.class;
                mSwitch.setVisibility(View.VISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                mSwitch.setChecked(false);
                break;
            case R.id.nav_fourth_fragment:
                fragmentClass = TimerFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                rollAllButton.setVisibility(View.INVISIBLE);
                break;
            default:
                fragmentClass = CounterFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
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


    /*
    public void rollDice(View view){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof DiceFragment){
            ((DiceFragment) tempFragment).rollDice(view);
        }
    }
    */

}


