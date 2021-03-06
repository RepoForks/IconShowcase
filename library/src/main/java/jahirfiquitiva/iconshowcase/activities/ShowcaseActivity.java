/*
 * Copyright (c) 2017 Jahir Fiquitiva
 *
 * Licensed under the CreativeCommons Attribution-ShareAlike
 * 4.0 International License. You may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *    http://creativecommons.org/licenses/by-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Special thanks to the project contributors and collaborators
 * 	https://github.com/jahirfiquitiva/IconShowcase#special-thanks
 */

package jahirfiquitiva.iconshowcase.activities;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.pitchedapps.capsule.library.changelog.ChangelogDialog;

import org.sufficientlysecure.donations.google.util.IabHelper;
import org.sufficientlysecure.donations.google.util.IabResult;
import org.sufficientlysecure.donations.google.util.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import jahirfiquitiva.iconshowcase.R;
import jahirfiquitiva.iconshowcase.activities.base.TasksActivity;
import jahirfiquitiva.iconshowcase.config.Config;
import jahirfiquitiva.iconshowcase.dialogs.ISDialogs;
import jahirfiquitiva.iconshowcase.fragments.KustomFragment;
import jahirfiquitiva.iconshowcase.fragments.PreviewsFragment;
import jahirfiquitiva.iconshowcase.fragments.RequestsFragment;
import jahirfiquitiva.iconshowcase.fragments.SettingsFragment;
import jahirfiquitiva.iconshowcase.fragments.WallpapersFragment;
import jahirfiquitiva.iconshowcase.fragments.ZooperFragment;
import jahirfiquitiva.iconshowcase.holders.lists.FullListHolder;
import jahirfiquitiva.iconshowcase.models.IconItem;
import jahirfiquitiva.iconshowcase.tasks.DownloadJSON;
import jahirfiquitiva.iconshowcase.utilities.color.ToolbarColorizer;
import jahirfiquitiva.iconshowcase.utilities.utils.PermissionsUtils;
import jahirfiquitiva.iconshowcase.utilities.utils.ThemeUtils;
import jahirfiquitiva.iconshowcase.utilities.utils.Utils;
import timber.log.Timber;

public class ShowcaseActivity extends TasksActivity {

    private IabHelper mHelper;

    private Drawer drawer;

    //TODO check if these are necessary
    private boolean iconsPicker = false, wallsPicker = false, allowShuffle = true, shuffleIcons =
            true;

    private long currentItem = -1;

    private int numOfIcons = 4, wallpaper = -1;

    //TODO do not save Dialog instance; use fragment tags
    private MaterialDialog settingsDialog;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    //TODO perhaps dynamically load the imageviews rather than predefining 8
    private ImageView icon1, icon2, icon3, icon4, icon5, icon6, icon7, icon8;
    private ImageView toolbarHeader;
    private Drawable wallpaperDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.onActivityCreateSetTheme(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ThemeUtils.onActivityCreateSetNavBar(this);
        }

        super.onCreate(savedInstanceState);

        String shortcut = getIntent().getStringExtra("shortcut");
        boolean openWallpapers = getIntent().getBooleanExtra("open_wallpapers", false) ||
                (shortcut != null && shortcut.equals("wallpapers_shortcut"));

        //TODO remove all this; donations will exist if they are configured
        WITH_DONATIONS_SECTION = getIntent().getBooleanExtra("enableDonations", false);
        DONATIONS_GOOGLE = getIntent().getBooleanExtra("enableGoogleDonations", false);
        DONATIONS_PAYPAL = getIntent().getBooleanExtra("enablePayPalDonations", false);
        DONATIONS_FLATTR = getIntent().getBooleanExtra("enableFlattrDonations", false);
        DONATIONS_BITCOIN = getIntent().getBooleanExtra("enableBitcoinDonations", false);

        WITH_LICENSE_CHECKER = getIntent().getBooleanExtra("enableLicenseCheck", false);
        WITH_INSTALLED_FROM_AMAZON = getIntent().getBooleanExtra("enableAmazonInstalls", false);
        ALLOW_APT_USE = getIntent().getBooleanExtra("enableAptoideUse", false);

        GOOGLE_PUBKEY = getIntent().getStringExtra("googlePubKey");

        getAction();

        shuffleIcons = getResources().getBoolean(R.bool.shuffle_toolbar_icons);

        try {
            String installer = Utils.getAppInstaller(this);
            if (installer.matches(Config.PLAY_STORE_INSTALLER) || installer.matches(Config
                    .PLAY_STORE_PACKAGE)) {
                installedFromPlayStore = true;
            }
        } catch (Exception e) {
            //Do nothing
        }

        setupDonations();

        //TODO dynamically add icons rather than defining 8 and hiding those that aren't used
        numOfIcons = getResources().getInteger(R.integer.toolbar_icons);

        icon1 = (ImageView) findViewById(R.id.iconOne);
        icon2 = (ImageView) findViewById(R.id.iconTwo);
        icon3 = (ImageView) findViewById(R.id.iconThree);
        icon4 = (ImageView) findViewById(R.id.iconFour);
        icon5 = (ImageView) findViewById(R.id.iconFive);
        icon6 = (ImageView) findViewById(R.id.iconSix);
        icon7 = (ImageView) findViewById(R.id.iconSeven);
        icon8 = (ImageView) findViewById(R.id.iconEight);

        capsulate().toolbar(R.id.toolbar).appBarLayout(R.id.appbar).coordinatorLayout(R.id
                .mainCoordinatorLayout);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        toolbarHeader = (ImageView) findViewById(R.id.toolbarHeader);

        thaAppName = getResources().getString(R.string.app_name);

        collapsingToolbarLayout.setTitle(thaAppName);
        ToolbarColorizer.setupCollapsingToolbarTextColors(this, collapsingToolbarLayout);
        setupDrawer(savedInstanceState);

        //Setup donations
        if (DONATIONS_GOOGLE) {
            final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper
                    .QueryInventoryFinishedListener() {

                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (inventory != null) {
                        Timber.i("IAP inventory exists");
                        for (String aGOOGLE_CATALOG : GOOGLE_CATALOG) {
                            Timber.i(aGOOGLE_CATALOG, "is", inventory.hasPurchase(aGOOGLE_CATALOG));
                            if (inventory.hasPurchase(aGOOGLE_CATALOG)) { //at least one donation
                                // value found, now premium
                                mIsPremium = true;
                            }
                        }
                    }
                    if (isPremium()) {
                        mGoogleCatalog = GOOGLE_CATALOG;
                    }
                }
            };

            mHelper = new IabHelper(ShowcaseActivity.this, GOOGLE_PUBKEY);
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        Timber.d("In-app Billing setup failed: ", result);
                        new MaterialDialog.Builder(ShowcaseActivity.this).title(R.string
                                .donations_error_title)
                                .content(R.string.donations_error_content)
                                .positiveText(android.R.string.ok)
                                .show();

                    } else {
                        mHelper.queryInventoryAsync(false, mGotInventoryListener);
                    }

                }
            });
        }

        if (savedInstanceState == null) {
            if (openWallpapers) {
                drawerItemClick(mDrawerMap.get(DrawerItem.WALLPAPERS));
            } else if (iconsPicker && mDrawerMap.containsKey(DrawerItem.PREVIEWS)) {
                drawerItemClick(mDrawerMap.get(DrawerItem.PREVIEWS));
            } else if (wallsPicker && mPrefs.isDashboardWorking() && mDrawerMap.containsKey
                    (DrawerItem.WALLPAPERS)) {
                drawerItemClick(mDrawerMap.get(DrawerItem.WALLPAPERS));
            } else if ((shortcut != null && shortcut.equals("apply_shortcut")) && mDrawerMap
                    .containsKey(DrawerItem.APPLY)) {
                drawerItemClick(mDrawerMap.get(DrawerItem.APPLY));
            } else if ((shortcut != null && shortcut.equals("request_shortcut")) && mDrawerMap
                    .containsKey(DrawerItem.REQUESTS)) {
                drawerItemClick(mDrawerMap.get(DrawerItem.REQUESTS));
            } else {
                if (mPrefs.getSettingsModified()) { //TODO remove this from preferences; this can
                    // be sent via bundle itself
                    //TODO check this
                    long settingsIdentifier = 0;
                    drawerItemClick(settingsIdentifier);
                } else {
                    currentItem = -1;
                    drawerItemClick(0);
                }
            }
        }

        //Load last, load all other data first
        startTasks(); //TODO check iconsPicker and wallsPicker booleans
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!iconsPicker && !wallsPicker) {
            setupToolbarHeader();
        }
        ToolbarColorizer.setupToolbarIconsAndTextsColors(this, cAppBarLayout, cToolbar);
        Utils.runLicenseChecker(this, WITH_LICENSE_CHECKER, GOOGLE_PUBKEY,
                WITH_INSTALLED_FROM_AMAZON, ALLOW_APT_USE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (settingsDialog != null) {
            settingsDialog.dismiss();
            settingsDialog = null;
        }
    }

    public Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(getFragmentId());
    }

    private void reloadFragment(DrawerItem dt) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction();

        fragmentTransaction.replace(getFragmentId(), dt.getFragment(), dt.getName());

        if (mPrefs.getAnimationsEnabled())
            fragmentTransaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter,
                    R.anim.pop_exit);
        fragmentTransaction.commit();
    }

    @SuppressWarnings("ResourceAsColor")
    private void setupDrawer(Bundle savedInstanceState) {
        getDrawerItems();

        DrawerBuilder drawerBuilder = new DrawerBuilder().withActivity(this)
                .withToolbar(cToolbar)
                // .withFireOnInitialOnClick(true)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            drawerItemClick(drawerItem.getIdentifier());
                        }
                        return false;
                    }
                });

        for (int position = 0; position < mDrawerItems.size(); position++) {
            DrawerItem item = mDrawerItems.get(position);
            if (item == DrawerItem.CREDITS) {
                drawerBuilder.addDrawerItems(new DividerDrawerItem());
            }
            mDrawerMap.put(item, position);
            drawerBuilder.addDrawerItems(
                    !item.isSecondary() ? DrawerItem.getPrimaryDrawerItem(this, item, position) :
                            DrawerItem.getSecondaryDrawerItem(this, item, position));
        }

        drawerBuilder.withSavedInstance(savedInstanceState);

        String headerAppName = "", headerAppVersion = "";

        boolean withDrawerTexts;

        withDrawerTexts = Config.get().devOptions() ? mPrefs.getDevDrawerTexts() :
                getResources().getBoolean(R.bool.with_drawer_texts);

        if (withDrawerTexts) {
            headerAppName = getResources().getString(R.string.app_long_name);
            headerAppVersion = "v " + Utils.getAppVersion(this);
        }

        AccountHeader drawerHeader = new AccountHeaderBuilder().withActivity(this)
                .withHeaderBackground(R.drawable.drawer_header)
                .withSelectionFirstLine(headerAppName)
                .withSelectionSecondLine(headerAppVersion)
                .withProfileImagesClickable(false)
                .withResetDrawerOnProfileListClick(false)
                .withSelectionListEnabled(false)
                .withSelectionListEnabledForSingleProfile(false)
                .withSavedInstance(savedInstanceState)
                .build();

        TextView drawerTitle = (TextView)
                drawerHeader.getView().findViewById(R.id.material_drawer_account_header_name);
        TextView drawerSubtitle = (TextView)
                drawerHeader.getView().findViewById(R.id.material_drawer_account_header_email);
        setTextAppearance(drawerTitle, R.style.DrawerTextsWithShadow);
        setTextAppearance(drawerSubtitle, R.style.DrawerTextsWithShadow);

        drawerBuilder.withAccountHeader(drawerHeader);

        drawer = drawerBuilder.build();
    }

    public void drawerItemClick(long id) {
        switchFragment(id, mDrawerItems.get((int) id));
    }

    private void switchFragment(long itemId, DrawerItem dt) {

        // Don't allow re-selection of the currently active item
        if (currentItem == itemId) return;

        currentItem = itemId;

        // TODO Make sure this works fine even after configuration changes
        if ((dt == DrawerItem.HOME) && (!iconsPicker && !wallsPicker)) {
            expandAppBar(mPrefs != null && mPrefs.getAnimationsEnabled());
        } else {
            collapseAppBar(mPrefs != null && mPrefs.getAnimationsEnabled());
        }

        if (dt == DrawerItem.HOME) {
            icon1.setVisibility(View.INVISIBLE);
            icon2.setVisibility(View.INVISIBLE);
            icon3.setVisibility(View.INVISIBLE);
            icon4.setVisibility(View.INVISIBLE);
        }

        //Fragment Switcher
        reloadFragment(dt);

        collapsingToolbarLayout.setTitle(dt.getName().equals("Home") ? Config.get().string(R
                .string.app_name) : getString(dt.getTitleID()));

        drawer.setSelection(itemId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (drawer != null) {
            outState = drawer.saveInstanceState(outState);
        }
        if (collapsingToolbarLayout != null && collapsingToolbarLayout.getTitle() != null) {
            outState.putString("toolbarTitle", collapsingToolbarLayout.getTitle().toString());
        }
        outState.putInt("currentSection", (int) currentItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (collapsingToolbarLayout != null) {
            ToolbarColorizer.setupCollapsingToolbarTextColors(this, collapsingToolbarLayout);
            collapsingToolbarLayout.setTitle(savedInstanceState.getString("toolbarTitle",
                    thaAppName));
        }
        drawerItemClick(savedInstanceState.getInt("currentSection"));
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else if (drawer != null && currentItem != 0 && !iconsPicker) {
            drawer.setSelection(0);
        } else if (drawer != null) {
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionsUtils.PERMISSION_REQUEST_CODE) {
            if (permissionGranted(grantResults)) {
                if (getCurrentFragment() instanceof RequestsFragment) {
                    ((RequestsFragment) getCurrentFragment()).startRequestProcess();
                } else if (getCurrentFragment() instanceof SettingsFragment) {
                    ((SettingsFragment) getCurrentFragment()).showFolderChooserDialog();
                } else if (getCurrentFragment() instanceof ZooperFragment) {
                    ((ZooperFragment) getCurrentFragment()).getAdapter().installAssets();
                }
            } else if (PermissionsUtils.getListener() != null) {
                PermissionsUtils.getListener().onPermissionGranted();
            } else {
                ISDialogs.showPermissionNotGrantedDialog(this);
            }
        } else {
            ISDialogs.showPermissionNotGrantedDialog(this);
        }
    }

    private boolean permissionGranted(int[] results) {
        return results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int i = item.getItemId();
        if (i == R.id.changelog) {
            if (includesIcons()) {
                ChangelogDialog.show(this, R.xml.changelog, new ChangelogDialog
                        .OnChangelogNeutralButtonClick() {
                    @Override
                    public void onNeutralButtonClick() {
                        drawerItemClick(mDrawerMap.get(DrawerItem.PREVIEWS));
                    }
                });
            } else {
                ChangelogDialog.show(this, R.xml.changelog, null);
            }
        } else if (i == R.id.refresh) {
            if (getCurrentFragment() instanceof WallpapersFragment) {
                FullListHolder.get().walls().clearList();
                DownloadJSON json = new DownloadJSON(this);
                json.setFragment(getCurrentFragment());
                if (getJsonTask() != null) {
                    getJsonTask().cancel(true);
                }
                setJsonTask(json);
                try {
                    json.execute();
                } catch (Exception e) {
                    // Do nothing
                }
            } else {
                Toast.makeText(this, "Can't perform this action from here.", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (i == R.id.columns) {
            if (getCurrentFragment() instanceof WallpapersFragment) {
                ISDialogs.showColumnsSelectorDialog(this, ((WallpapersFragment)
                        getCurrentFragment()));
            } else {
                Toast.makeText(this, "Can't perform this action from here.", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (i == R.id.select_all) {
            if (getCurrentFragment() instanceof RequestsFragment) {
                if (((RequestsFragment) getCurrentFragment()).getAdapter() != null && (
                        (RequestsFragment) getCurrentFragment()).getAdapter().getItemCount() > 0) {
                    ((RequestsFragment) getCurrentFragment()).getAdapter().selectOrDeselectAll();
                }
            } else {
                Toast.makeText(this, "Can't perform this action from here.", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void resetFragment(DrawerItem item) {
        switch (item) {
            case PREVIEWS:
                if (getCurrentFragment() instanceof PreviewsFragment) {
                    reloadFragment(item);
                }
                break;
            case ZOOPER:
                if (getCurrentFragment() instanceof ZooperFragment) {
                    reloadFragment(item);
                }
                break;
            case KUSTOM:
                if (getCurrentFragment() instanceof KustomFragment) {
                    reloadFragment(item);
                }
                break;
        }
    }

    @Override
    protected int getFragmentId() {
        return R.id.main;
    }

    /**
     * Gets the fab ID
     *
     * @return fabID
     */
    @Override
    protected int getFabId() {
        return R.id.fab;
    }

    /**
     * Gets your layout ID for the activity
     *
     * @return layoutID
     */
    @Override
    protected int getContentViewId() {
        return R.layout.showcase_activity;
    }

    @SuppressWarnings("deprecation")
    private void setTextAppearance(TextView text, @StyleRes int style) {
        if (text != null) {
            if (Build.VERSION.SDK_INT < 23) {
                text.setTextAppearance(this, style);
            } else {
                text.setTextAppearance(style);
            }
        }
    }

    private void getAction() {
        if (getIntent() == null || getIntent().getAction() == null) return;
        switch (getIntent().getAction()) {
            case Config.ADW_ACTION:
            case Config.TURBO_ACTION:
            case Config.NOVA_ACTION:
            case Intent.ACTION_PICK:
            case Intent.ACTION_GET_CONTENT:
                iconsPicker = true;
                break;
            case Intent.ACTION_SET_WALLPAPER:
                wallsPicker = true;
                break;
        }
    }

    public void setupIcons() {
        if ((FullListHolder.get().home().isEmpty()) || (!(includesIcons()))) return;

        ArrayList<IconItem> finalIconsList = new ArrayList<>();

        if (allowShuffle && shuffleIcons) {
            Collections.shuffle(FullListHolder.get().home().getList());
        }

        int i = 0;

        while (i < numOfIcons) {
            finalIconsList.add(FullListHolder.get().home().getList().get(i));
            i++;
        }

        icon1.setImageResource(finalIconsList.get(0).getResId());
        icon2.setImageResource(finalIconsList.get(1).getResId());
        icon3.setImageResource(finalIconsList.get(2).getResId());
        icon4.setImageResource(finalIconsList.get(3).getResId());

        if (numOfIcons == 6) {
            icon5.setImageResource(finalIconsList.get(4).getResId());
            icon6.setImageResource(finalIconsList.get(5).getResId());
        } else if (numOfIcons == 8) {
            icon5.setImageResource(finalIconsList.get(4).getResId());
            icon6.setImageResource(finalIconsList.get(5).getResId());
            icon7.setImageResource(finalIconsList.get(6).getResId());
            icon8.setImageResource(finalIconsList.get(7).getResId());
        }

        allowShuffle = false;

    }

    public void animateIcons(int delay) {
        if (!(includesIcons())) return;

        if (!iconsPicker && !wallsPicker) {
            switch (numOfIcons) {
                case 8:
                    if (icon7 != null) {
                        icon7.setVisibility(View.VISIBLE);
                    }
                    if (icon8 != null) {
                        icon8.setVisibility(View.VISIBLE);
                    }
                case 6:
                    if (icon5 != null) {
                        icon5.setVisibility(View.VISIBLE);
                    }
                    if (icon6 != null) {
                        icon6.setVisibility(View.VISIBLE);
                    }
                case 4:
                    if (icon1 != null) {
                        icon1.setVisibility(View.VISIBLE);
                    }
                    if (icon2 != null) {
                        icon2.setVisibility(View.VISIBLE);
                    }
                    if (icon3 != null) {
                        icon3.setVisibility(View.VISIBLE);
                    }
                    if (icon4 != null) {
                        icon4.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }

        if (mPrefs.getAnimationsEnabled()) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.bounce);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playIconsAnimations(anim);
                }
            }, delay);
        }

    }

    private void playIconsAnimations(Animation anim) {
        if (!(includesIcons())) return;

        icon1.startAnimation(anim);
        icon2.startAnimation(anim);
        icon3.startAnimation(anim);
        icon4.startAnimation(anim);

        switch (numOfIcons) {
            case 6:
                icon5.startAnimation(anim);
                icon6.startAnimation(anim);
                break;
            case 8:
                icon5.startAnimation(anim);
                icon6.startAnimation(anim);
                icon7.startAnimation(anim);
                icon8.startAnimation(anim);
                break;
        }
    }

    public void setupToolbarHeader() {
        try {
            if (Config.get().userWallpaperInToolbar() && mPrefs
                    .getWallpaperAsToolbarHeaderEnabled()) {
                WallpaperManager wm = WallpaperManager.getInstance(this);
                if (wm != null) {
                    Drawable currentWallpaper = wm.getFastDrawable();
                    if (currentWallpaper != null) {
                        toolbarHeader.setAlpha(0.9f);
                        toolbarHeader.setImageDrawable(currentWallpaper);
                        wallpaperDrawable = currentWallpaper;
                    }
                }
            } else {
                String defPicture = getResources().getString(R.string.toolbar_picture);
                if (defPicture.length() > 0) {
                    int res = getResources().getIdentifier(defPicture, "drawable", getPackageName
                            ());
                    if (res != 0) {
                        wallpaperDrawable = ContextCompat.getDrawable(this, wallpaper);
                        toolbarHeader.setImageDrawable(wallpaperDrawable);
                    }
                } else {
                    String[] wallpapers = getResources().getStringArray(R.array.wallpapers);
                    if (wallpapers.length > 0) {
                        int res;
                        ArrayList<Integer> wallpapersArray = new ArrayList<>();
                        for (String wallpaper : wallpapers) {
                            res = getResources().getIdentifier(wallpaper, "drawable",
                                    getPackageName());
                            if (res != 0) {
                                wallpapersArray.add(res);
                            }
                        }
                        Random random = new Random();
                        if (wallpaper == -1) {
                            wallpaper = random.nextInt(wallpapersArray.size());
                        }
                        wallpaperDrawable = ContextCompat.getDrawable(this, wallpapersArray.get
                                (wallpaper));
                        toolbarHeader.setImageDrawable(wallpaperDrawable);
                    }
                }
            }
            toolbarHeader.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // Do nothing
        }
    }

    private void setupDonations() {
        //donations stuff

        if (installedFromPlayStore) {
            // Disable donation methods not allowed by Google
            DONATIONS_PAYPAL = false;
            DONATIONS_FLATTR = false;
            DONATIONS_BITCOIN = false;
        }

        //google
        if (DONATIONS_GOOGLE) {
            GOOGLE_CATALOG = getResources().getStringArray(R.array.google_donations_items);
            mGoogleCatalog = GOOGLE_CATALOG;
            GOOGLE_CATALOG_VALUES = getResources().getStringArray(R.array.google_donations_catalog);

            try {
                if (!(GOOGLE_PUBKEY.length() > 50) || !(GOOGLE_CATALOG_VALUES.length > 0) || !
                        (GOOGLE_CATALOG.length == GOOGLE_CATALOG_VALUES.length)) {
                    DONATIONS_GOOGLE = false; //google donations setup is incorrect
                }
            } catch (Exception e) {
                DONATIONS_GOOGLE = false;
            }

        }

        //paypal
        if (DONATIONS_PAYPAL) {
            PAYPAL_USER = getResources().getString(R.string.paypal_email);
            PAYPAL_CURRENCY_CODE = getResources().getString(R.string.paypal_currency_code);
            if (!(PAYPAL_USER.length() > 5) || !(PAYPAL_CURRENCY_CODE.length() > 1)) {
                DONATIONS_PAYPAL = false; //paypal content incorrect
            }
        }

        if (WITH_DONATIONS_SECTION) {
            WITH_DONATIONS_SECTION = DONATIONS_GOOGLE || DONATIONS_PAYPAL || DONATIONS_FLATTR ||
                    DONATIONS_BITCOIN; //if one of the donations are enabled,
            // then the section is enabled
        }
    }

    private boolean isPremium() {
        return mIsPremium;
    }

    public void setSettingsDialog(MaterialDialog settingsDialog) {
        this.settingsDialog = settingsDialog;
    }

    public Toolbar getToolbar() {
        return cToolbar;
    }

    public AppBarLayout getAppbar() {
        return cAppBarLayout;
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public boolean includesZooper() {
        return WITH_ZOOPER_SECTION;
    }

    public boolean includesIcons() {
        return mDrawerMap != null && mDrawerMap.containsKey(DrawerItem.PREVIEWS);
    }

    public boolean includesWallpapers() {
        return mDrawerMap != null && mDrawerMap.containsKey(DrawerItem.WALLPAPERS);
    }

    public boolean allowShuffle() {
        return allowShuffle;
    }

    @SuppressWarnings("SameParameterValue")
    public void setAllowShuffle(boolean newValue) {
        this.allowShuffle = newValue;
    }

    public boolean isIconsPicker() {
        return iconsPicker;
    }

    public boolean isWallsPicker() {
        return wallsPicker;
    }

    public Drawable getWallpaperDrawable() {
        return wallpaperDrawable;
    }

    public long getPreviewsId() {
        return mDrawerMap != null ? mDrawerMap.get(DrawerItem.PREVIEWS) : -1;
    }

}