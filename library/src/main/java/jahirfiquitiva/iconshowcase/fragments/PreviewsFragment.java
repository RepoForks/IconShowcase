/*
 * Copyright (c) 2016 Jahir Fiquitiva
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

package jahirfiquitiva.iconshowcase.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.Locale;

import jahirfiquitiva.iconshowcase.R;
import jahirfiquitiva.iconshowcase.activities.ShowcaseActivity;
import jahirfiquitiva.iconshowcase.fragments.base.FragmentStatePagerAdapter;
import jahirfiquitiva.iconshowcase.models.IconsCategory;
import jahirfiquitiva.iconshowcase.utilities.ThemeUtils;
import jahirfiquitiva.iconshowcase.utilities.color.ToolbarColorizer;
import jahirfiquitiva.iconshowcase.utilities.color.ToolbarTinter;

@SuppressWarnings("ResourceAsColor")
public class PreviewsFragment extends NoFabBaseFragment {

    private int mLastSelected = 0;
    private ViewPager mPager;
    private String[] tabs;
    private TabLayout mTabs;
    private SearchView mSearchView;
    private ArrayList<IconsCategory> mCategories;
    private MenuItem mSearchItem;

    private static final String categoryListKey = "preview_categories";

    public static PreviewsFragment newInstance (@Nullable ArrayList<IconsCategory> categories) {
        PreviewsFragment fragment = new PreviewsFragment();
        if (categories == null) return fragment;
        Bundle args = new Bundle();
        args.putParcelableArrayList(categoryListKey, categories);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Bundle args = getArguments();

        if (args == null || !args.containsKey(categoryListKey)) return loadingView(inflater, container);

        View layout = inflater.inflate(R.layout.icons_preview_section, container, false);

        mCategories = args.getParcelableArrayList(categoryListKey);

        //TODO Check if ViewPager is smooth enough
        mPager = (ViewPager) layout.findViewById(R.id.pager);
        mPager.setAdapter(new IconsPagerAdapter(getChildFragmentManager()));
        mPager.setOffscreenPageLimit(mCategories.size() - 1 < 1 ? 1 : mCategories.size() - 1);
        mTabs = (TabLayout) getActivity().findViewById(R.id.tabs);
        createTabs();

        return layout;
    }

    @Override
    public void onCreate (@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated (View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int iconsColor = ThemeUtils.darkTheme ?
                ContextCompat.getColor(getActivity(), R.color.toolbar_text_dark) :
                ContextCompat.getColor(getActivity(), R.color.toolbar_text_light);

        if (getActivity() != null && mSearchItem != null) {
            ToolbarColorizer.tintSearchView(getActivity(),
                    ((ShowcaseActivity) getActivity()).getToolbar(), mSearchItem, mSearchView,
                    iconsColor);
        }
    }

    private void createTabs () {
        mTabs.removeAllTabs();
        mTabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mPager));
        mPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs) {
            @Override
            public void onPageSelected (int position) {
                super.onPageSelected(position);
                if (mLastSelected > -1) {
                    IconsFragment frag = (IconsFragment) getChildFragmentManager().findFragmentByTag("page:" + mLastSelected);
                    if (frag != null)
                        frag.performSearch(null);
                }
                mLastSelected = position;
                if (mSearchView != null && getActivity() != null)
                    mSearchView.setQueryHint(getString(R.string.search_x, tabs[mLastSelected]));
                if (getActivity() != null)
                    getActivity().invalidateOptionsMenu();
            }
        });
        for (IconsCategory category : mCategories) {
            mTabs.addTab(mTabs.newTab().setText(category.getCategoryName()));
        }
        mTabs.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView () {
        super.onDestroyView();
        if (mTabs != null) mTabs.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setQueryHint(getString(R.string.search_x, tabs[mLastSelected]));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit (String s) {
                search(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange (String s) {
                search(s);
                return false;
            }

            private void search (String s) {
                IconsFragment frag =
                        (IconsFragment) getChildFragmentManager().findFragmentByTag("page:" +
                                mPager.getCurrentItem());
                if (frag != null)
                    frag.performSearch(s);
            }
        });

        mSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        int iconsColor = ThemeUtils.darkTheme ?
                ContextCompat.getColor(getActivity(), R.color.toolbar_text_dark) :
                ContextCompat.getColor(getActivity(), R.color.toolbar_text_light);

        ToolbarTinter.on(menu)
                .setIconsColor(iconsColor)
                .forceIcons()
                .reapplyOnChange(true)
                .apply(getActivity());

    }

    class IconsPagerAdapter extends FragmentStatePagerAdapter {

        public IconsPagerAdapter (FragmentManager fm) {
            super(fm);
            String[] tabsNames = new String[mCategories.size()];
            for (int i = 0; i < tabsNames.length; i++) {
                tabsNames[i] = mCategories.get(i).getCategoryName();
            }
            tabs = tabsNames;
        }

        @Override
        public Fragment getItem (int position) {
            return IconsFragment.newInstance(mCategories.get(position));
        }

        @Override
        public CharSequence getPageTitle (int position) {
            return tabs[position].toUpperCase(Locale.getDefault());
        }

        @Override
        public int getCount () {
            return tabs.length;
        }
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
        // Save current position
        savedInstanceState.putInt("lastSelected", mLastSelected);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore last selected position
            mLastSelected = savedInstanceState.getInt("lastSelected", 0);
        }
    }
}