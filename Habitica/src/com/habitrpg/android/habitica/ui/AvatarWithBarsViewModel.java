package com.habitrpg.android.habitica.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.databinding.ValueBarBinding;
import com.habitrpg.android.habitica.events.BoughtGemsEvent;
import com.habitrpg.android.habitica.events.commands.OpenGemPurchaseFragmentCommand;
import com.habitrpg.android.habitica.userpicture.UserPicture;
import com.magicmicky.habitrpgwrapper.lib.models.HabitRPGUser;
import com.magicmicky.habitrpgwrapper.lib.models.Stats;

import de.greenrobot.event.EventBus;

/**
 * Created by Negue on 14.06.2015.
 */
public class AvatarWithBarsViewModel implements View.OnClickListener {
    private ValueBarBinding hpBar;
    private ValueBarBinding xpBar;
    private ValueBarBinding mpBar;

    private ImageView image;

    private android.content.res.Resources res;

    private Context context;

    private TextView lvlText, goldText, silverText, gemsText;
    private HabitRPGUser userObject;
    private UserPicture userPicture;

    private int cachedMaxHealth, cachedMaxExp, cachedMaxMana;

    public AvatarWithBarsViewModel(Context context, View v) {
        this.context = context;

        res = context.getResources();

        if (v == null) {
            Log.w("AvatarWithBarsViewModel", "View is null");
            return;
        }

        lvlText = (TextView) v.findViewById(R.id.lvl_tv);
        goldText = (TextView) v.findViewById(R.id.gold_tv);
        silverText = (TextView) v.findViewById(R.id.silver_tv);
        gemsText = (TextView) v.findViewById(R.id.gems_tv);
        View hpBarView = v.findViewById(R.id.hpBar);

        image = (ImageView) v.findViewById(R.id.IMG_ProfilePicture);
        hpBar = DataBindingUtil.bind(hpBarView);
        xpBar = DataBindingUtil.bind(v.findViewById(R.id.xpBar));
        mpBar = DataBindingUtil.bind(v.findViewById(R.id.mpBar));

        setHpBarData(0, 50);
        setXpBarData(0, 1);
        setMpBarData(0, 1);

        gemsText.setClickable(true);
        gemsText.setOnClickListener(this);
        this.userPicture = new UserPicture(this.context);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void updateData(HabitRPGUser user) {
        userObject = user;

        Stats stats = user.getStats();

        String userClass = "";
        int gp = (stats.getGp().intValue());
        int sp = (int) ((stats.getGp() - gp) * 100);

        setHpBarData(stats);
        setXpBarData(stats.getExp().floatValue(), stats.getToNextLevel());
        setMpBarData(stats.getMp().floatValue(), stats.getMaxMP());

        userPicture.setUser(user);
        userPicture.setPictureOn(image);

        if (stats.get_class() != null) {
            userClass += stats.getCleanedClassName();
        }

        mpBar.valueBarLayout.setVisibility((stats.get_class() == null || stats.getLvl() < 10) ? View.GONE : View.VISIBLE);

        lvlText.setText("Lvl " + user.getStats().getLvl() + " - " + userClass);
        Drawable drawable;
        switch (stats.get_class()) {
            case warrior:
                drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_header_warrior, null);

                break;
            case rogue:
                drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_header_rogue, null);
                break;
            case wizard:
                drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_header_mage, null);

                break;
            case healer:
                drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_header_healer, null);

                break;
            case base:
            default:
                drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_header_warrior, null);

        }
        drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                drawable.getMinimumHeight());
        lvlText.setCompoundDrawables(drawable, null, null, null);

        goldText.setText(String.valueOf(gp));
        silverText.setText(String.valueOf(sp));

        Double gems = user.getBalance() * 4;
        gemsText.setText(String.valueOf(gems.intValue()));
    }

    public static void setHpBarData(ValueBarBinding valueBar, Stats stats, Context ctx) {
        int maxHP = stats.getMaxHealth();
        if (maxHP == 0) {
            maxHP = 50;
        }

        setValueBar(valueBar, stats.getHp().floatValue(), maxHP, ctx.getString(R.string.HP_default), ctx.getResources().getColor(R.color.hpColor), R.drawable.ic_header_heart);
    }

    public void setHpBarData(Stats stats) {
        setHpBarData(hpBar, stats, context);
    }

    public void setHpBarData(float value, int valueMax){
        if (valueMax == 0) {
            valueMax = cachedMaxHealth;
        } else {
            cachedMaxHealth = valueMax;
        }
        setValueBar(hpBar, value, valueMax, context.getString(R.string.HP_default), context.getResources().getColor(R.color.hpColor), R.drawable.ic_header_heart);
    }

    public void setXpBarData(float value, int valueMax){
        if (valueMax == 0) {
            valueMax = cachedMaxExp;
        } else {
            cachedMaxExp = valueMax;
        }
        setValueBar(xpBar, value, valueMax, context.getString(R.string.XP_default), context.getResources().getColor(R.color.xpColor), R.drawable.ic_header_exp);
    }

    public void setMpBarData(float value, int valueMax){
        if (valueMax == 0) {
            valueMax = cachedMaxMana;
        } else {
            cachedMaxMana = valueMax;
        }
        setValueBar(mpBar, value, valueMax, context.getString(R.string.MP_default), context.getResources().getColor(R.color.mpColor), R.drawable.ic_header_magic);
    }

    // Layout_Weight don't accepts 0.7/0.3 to have 70% filled instead it shows the 30% , so I had to switch the values
    // but on a 1.0/0.0 which switches to 0.0/1.0 it shows the blank part full size...
    private static void setValueBar(ValueBarBinding valueBar, float value, float valueMax, String description, int color, int icon) {
        value = (float) Math.ceil(value);
        double percent = Math.min(1, value / valueMax);

        if (percent == 1) {
            valueBar.setWeightToShow(1);
            valueBar.setWeightToHide(0);
        } else {
            valueBar.setWeightToShow((float) percent);
            valueBar.setWeightToHide((float) (1 - percent));
        }

        valueBar.setText((int) value + "/" + (int) valueMax);
        valueBar.setDescription(description);
        valueBar.setBarForegroundColor(color);
        valueBar.icHeader.setImageResource(icon);
    }

    public void onEvent(BoughtGemsEvent gemsEvent){
        Double gems = userObject.getBalance() * 4;
        gems += gemsEvent.NewGemsToAdd;
        gemsText.setText(String.valueOf(gems.intValue()));
    }

    @Override
    public void onClick(View view) {
        // Gems Clicked

        EventBus.getDefault().post(new OpenGemPurchaseFragmentCommand());
    }
}
