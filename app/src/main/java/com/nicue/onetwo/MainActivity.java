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
    private TextView actionTitle;
    private NavigationView nvDrawer;
    private Fragment firstFragment;
    private boolean switchOn = false;

    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mSwitch = (Switch) toolbar.findViewById(R.id.toolbar_switch);
        actionTitle = (TextView) toolbar.findViewById(R.id.custom_toolbar_title);
        mSwitch.setOnCheckedChangeListener(this);
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
            actionTitle.setText("Counter");
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
        if (findViewById(R.id.remove_button).equals(v.getId())){
            //CounterFragment.deleteObj(v);
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
                break;
            case R.id.nav_second_fragment:
                fragmentClass = DiceFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
                break;
            case R.id.nav_third_fragment:
                fragmentClass = ChooserFragment.class;
                mSwitch.setVisibility(View.VISIBLE);
                mSwitch.setChecked(false);
                break;
            case R.id.nav_fourth_fragment:
                fragmentClass = TimerFragment.class;
                mSwitch.setVisibility(View.INVISIBLE);
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
        actionTitle.setText(menuItem.getTitle());
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
    /*
    public void rollDice(View view){
        Fragment tempFragment = getSupportFragmentManager().findFragmentById(R.id.m_content);

        if (tempFragment instanceof DiceFragment){
            ((DiceFragment) tempFragment).rollDice(view);
        }
    }
    */

}


