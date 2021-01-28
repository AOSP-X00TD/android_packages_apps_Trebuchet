/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.search;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import android.app.search.SearchTarget;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.PackageItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A full width representation of {@link SearchResultIcon} with a secondary label and inline
 * SearchTargets
 */
public class SearchResultIconRow extends LinearLayout implements SearchTargetHandler {

    public static final int MAX_INLINE_ITEMS = 2;

    protected final Launcher mLauncher;
    private final LauncherAppState mLauncherAppState;
    private SearchResultIcon mResultIcon;

    private TextView mTitleView;
    private TextView mSubTitleView;
    private final SearchResultIcon[] mInlineIcons = new SearchResultIcon[MAX_INLINE_ITEMS];

    private PackageItemInfo mProviderInfo;

    public SearchResultIconRow(Context context) {
        this(context, null, 0);
    }

    public SearchResultIconRow(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchResultIconRow(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(getContext());
        mLauncherAppState = LauncherAppState.getInstance(getContext());
    }

    protected int getIconSize() {
        return mLauncher.getDeviceProfile().allAppsIconSizePx;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int iconSize = getIconSize();

        mResultIcon = findViewById(R.id.icon);
        mTitleView = findViewById(R.id.title);
        mSubTitleView = findViewById(R.id.subtitle);
        mSubTitleView.setVisibility(GONE);
        mResultIcon.getLayoutParams().height = iconSize;
        mResultIcon.getLayoutParams().width = iconSize;
        mResultIcon.setTextVisibility(false);

        mInlineIcons[0] = findViewById(R.id.shortcut_0);
        mInlineIcons[1] = findViewById(R.id.shortcut_1);
        for (SearchResultIcon inlineIcon : mInlineIcons) {
            inlineIcon.getLayoutParams().width = getIconSize();
            // TODO: inlineIcon.setOnClickListener();
        }

        setOnClickListener(mResultIcon);
        setOnLongClickListener(mResultIcon);
    }

    @Override
    public void apply(SearchTarget parentTarget, List<SearchTarget> children) {
        mResultIcon.applySearchTarget(parentTarget, children, this::onItemInfoCreated);
        if (parentTarget.getShortcutInfo() != null) {
            updateWithShortcutInfo(parentTarget.getShortcutInfo());
        } else if (parentTarget.getSearchAction() != null) {
            showSubtitleIfNeeded(parentTarget.getSearchAction().getSubtitle());
        }
        showInlineItems(children);
    }

    @Override
    public boolean quickSelect() {
        this.performClick();
        return true;
    }

    private void updateWithShortcutInfo(ShortcutInfo shortcutInfo) {
        PackageItemInfo packageItemInfo = new PackageItemInfo(shortcutInfo.getPackage());
        if (packageItemInfo.equals(mProviderInfo)) return;
        MODEL_EXECUTOR.post(() -> {
            mLauncherAppState.getIconCache().getTitleAndIconForApp(packageItemInfo, true);
            MAIN_EXECUTOR.post(() -> {
                showSubtitleIfNeeded(packageItemInfo.title);
                mProviderInfo = packageItemInfo;
            });
        });
    }

    protected void showSubtitleIfNeeded(CharSequence subTitle) {
        if (!TextUtils.isEmpty(subTitle)) {
            mSubTitleView.setText(subTitle);
            mSubTitleView.setVisibility(VISIBLE);
        } else {
            mSubTitleView.setVisibility(GONE);
        }
    }

    protected void showInlineItems(List<SearchTarget> children) {
        for (int i = 0; i < MAX_INLINE_ITEMS; i++) {
            if (i < children.size()) {
                mInlineIcons[i].apply(children.get(0), new ArrayList<>());
                mInlineIcons[i].setVisibility(VISIBLE);
            } else {
                mInlineIcons[i].setVisibility(GONE);
            }
        }
    }

    protected void onItemInfoCreated(ItemInfoWithIcon info) {
        setTag(info);
        mTitleView.setText(info.title);
    }

    @Override
    public void onClick(View view) {
        // do nothing.
    }

    @Override
    public boolean onLongClick(View view) {
        // do nothing.
        return false;
    }
}