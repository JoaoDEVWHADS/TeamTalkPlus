
package dk.bearware.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import androidx.viewpager.widget.ViewPager;

import java.util.Locale;

import android.content.Context;

import dk.bearware.AudioCodec;
import dk.bearware.AudioConfig;
import dk.bearware.Codec;
import dk.bearware.OpusCodec;
import dk.bearware.OpusConstants;
import dk.bearware.SpeexCodec;
import dk.bearware.SpeexConstants;
import dk.bearware.SpeexVBRCodec;
import dk.bearware.backend.TeamTalkConstants;
import dk.bearware.data.MapAdapter;

public class AudioCodecActivity extends AppCompatActivity implements
    ActionBar.TabListener {

    static final int TAB_OPUS       = 0,
                     TAB_SPEEX      = 1,
                     TAB_SPEEXVBR   = 2,
                     TAB_NOAUDIO    = 3,

                     TAB_COUNT      = 4;

    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;

    AudioCodec audiocodec;
    AudioConfig audiocfg;
    AccessibilityAssistant accessibilityAssistant;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(dk.bearware.gui.LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audiocodec);
        EdgeToEdgeHelper.enableEdgeToEdge(this);

        audiocodec = Utils.getAudioCodec(this.getIntent());
        audiocfg = Utils.getAudioConfig(this.getIntent());

        int tab_index = 0;
        switch(audiocodec.nCodec) {
            case Codec.OPUS_CODEC :
                tab_index = TAB_OPUS;
                break;
            case Codec.SPEEX_CODEC :
                tab_index = TAB_SPEEX;
                break;
            case Codec.SPEEX_VBR_CODEC :
                tab_index = TAB_SPEEXVBR;
                break;
            case Codec.NO_CODEC :
                tab_index = TAB_NOAUDIO;
                break;
        }

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                accessibilityAssistant.setVisiblePage(position);
                Fragment frag = mSectionsPagerAdapter.getActiveFragment(position);
                if (frag != null && frag.getView() != null) {
                    if (frag instanceof OPUSFragment)
                        ((OPUSFragment) frag).exchangeOpusCodec(frag.getView(), false);
                    else if (frag instanceof SpeexFragment)
                        ((SpeexFragment) frag).exchangeSpeexCodec(frag.getView(), false);
                    else if (frag instanceof SpeexVBRFragment)
                        ((SpeexVBRFragment) frag).exchangeSpeexVBRCodec(frag.getView(), false);
                }
            }
        });

        for(int i = 0;i < mSectionsPagerAdapter.getCount();i++) {

            actionBar.addTab(actionBar.newTab().setText(
                mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        mViewPager.setCurrentItem(tab_index);

        accessibilityAssistant = new AccessibilityAssistant(this);
        accessibilityAssistant.setVisiblePage(tab_index);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            android.view.View focused = getCurrentFocus();
            android.widget.SeekBar seekBar = null;
            if (focused instanceof android.widget.SeekBar) {
                seekBar = (android.widget.SeekBar) focused;
            } else if (!(focused instanceof android.widget.EditText)) {
                // If not typing, try to find the first visible SeekBar
                seekBar = findVisibleSeekBar(getWindow().getDecorView());
            }

            if (seekBar != null && seekBar.getVisibility() == android.view.View.VISIBLE) {
                int progress = seekBar.getProgress();
                if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                    if (progress < seekBar.getMax()) {
                        seekBar.setProgress(progress + 1);
                    }
                } else {
                    if (progress > 0) {
                        seekBar.setProgress(progress - 1);
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private android.widget.SeekBar findVisibleSeekBar(android.view.View view) {
        if (view instanceof android.widget.SeekBar && view.getVisibility() == android.view.View.VISIBLE) {
            return (android.widget.SeekBar) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.SeekBar sb = findVisibleSeekBar(group.getChildAt(i));
                if (sb != null) return sb;
            }
        }
        return null;
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            android.view.View focused = getCurrentFocus();
            if (focused instanceof android.widget.SeekBar || (focused != null && !(focused instanceof android.widget.EditText) && findVisibleSeekBar(getWindow().getDecorView()) != null)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            int i = mViewPager.getCurrentItem();
            Fragment frag = mSectionsPagerAdapter.getActiveFragment(i);
            if (frag != null && frag.getView() != null) {
                switch (i) {
                    case TAB_OPUS: {
                        OPUSFragment opusfrag = (OPUSFragment) frag;
                        audiocodec.opus = opusfrag.exchangeOpusCodec(opusfrag.getView(), true);
                        audiocodec.nCodec = Codec.OPUS_CODEC;
                        break;
                    }
                    case TAB_SPEEX: {
                        SpeexFragment spxfrag = (SpeexFragment) frag;
                        audiocodec.speex = spxfrag.exchangeSpeexCodec(spxfrag.getView(), true);
                        audiocodec.nCodec = Codec.SPEEX_CODEC;
                        break;
                    }
                    case TAB_SPEEXVBR: {
                        SpeexVBRFragment spxfrag = (SpeexVBRFragment) frag;
                        audiocodec.speex_vbr = spxfrag.exchangeSpeexVBRCodec(spxfrag.getView(), true);
                        audiocodec.nCodec = Codec.SPEEX_VBR_CODEC;
                        break;
                    }
                    case TAB_NOAUDIO: {
                        audiocodec.nCodec = Codec.NO_CODEC;
                        break;
                    }
                }
            } else {
                // Fallback to current audiocodec.nCodec if fragment is missing (should not happen)
                Log.e("AudioCodecActivity", "Active fragment or view is null for tab " + i);
            }

            Intent intent = getIntent();
            intent = Utils.putAudioConfig(Utils.putAudioCodec(intent, audiocodec), audiocfg);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab,
        FragmentTransaction fragmentTransaction) {

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab,
        FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab,
        FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        SparseArray<Fragment> fragments = new SparseArray<>();

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                default:
                case TAB_OPUS:
                    return new OPUSFragment();
                case TAB_SPEEX:
                    return new SpeexFragment();
                case TAB_SPEEXVBR:
                    return new SpeexVBRFragment();
                case TAB_NOAUDIO:
                    return new NoAudioFragment();
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            fragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            fragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getActiveFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch(position) {
                case TAB_OPUS :
                    return getString(R.string.title_section_opus).toUpperCase(l);
                case TAB_SPEEX :
                    return getString(R.string.title_section_speex).toUpperCase(l);
                case TAB_SPEEXVBR :
                    return getString(R.string.title_section_speexvbr).toUpperCase(l);
                case TAB_NOAUDIO :
                    return getString(R.string.title_section_noaudio).toUpperCase(l);
            }
            return null;
        }
    }

    public static class OPUSFragment extends Fragment {

        OpusCodec opuscodec;
        AudioConfig audiocfg;
        MapAdapter appMap, srMap, audMap, fsMap;
        MapAdapter bitrateModeMap, txintervalModeMap, fsmsecModeMap;

        public OPUSFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            opuscodec = ((AudioCodecActivity)activity).audiocodec.opus;
            audiocfg = ((AudioCodecActivity)activity).audiocfg;
            appMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            srMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            audMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            fsMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);

            appMap.addPair(getString(R.string.opus_app_voip), OpusConstants.OPUS_APPLICATION_VOIP);
            appMap.addPair(getString(R.string.opus_app_music), OpusConstants.OPUS_APPLICATION_AUDIO);

            srMap.addPair(getString(R.string.sample_rate_8khz), 8000);
            srMap.addPair(getString(R.string.sample_rate_12khz), 12000);
            srMap.addPair(getString(R.string.sample_rate_16khz), 16000);
            srMap.addPair(getString(R.string.sample_rate_24khz), 24000);
            srMap.addPair(getString(R.string.sample_rate_48khz), 48000);

            audMap.addPair(getString(R.string.audio_mono), 1);
            audMap.addPair(getString(R.string.audio_stereo), 2);

            fsMap.addPair(getString(R.string.framesize_default), TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC);
            fsMap.addPair(getString(R.string.framesize_ms, "2.5"), OpusConstants.OPUS_MIN_FRAMESIZE);
            fsMap.addPair(getString(R.string.framesize_ms, "5"), 5);
            fsMap.addPair(getString(R.string.framesize_ms, "10"), 10);
            fsMap.addPair(getString(R.string.framesize_ms, "20"), 20);
            fsMap.addPair(getString(R.string.framesize_ms, "40"), 40);
            fsMap.addPair(getString(R.string.framesize_ms, "60"), OpusConstants.OPUS_MAX_FRAMESIZE);
            fsMap.addPair(getString(R.string.framesize_ms, "80"), 80);
            fsMap.addPair(getString(R.string.framesize_ms, "100"), 100);
            fsMap.addPair(getString(R.string.framesize_ms, "120"), OpusConstants.OPUS_REALMAX_FRAMESIZE);

            bitrateModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            bitrateModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            bitrateModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            txintervalModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            fsmsecModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            fsmsecModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            fsmsecModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_audiocodec_opus,
                                        container, false);
            ((AudioCodecActivity)getActivity()).accessibilityAssistant.registerPage(rootView, TAB_OPUS);
            exchangeOpusCodec(rootView, false);

            return rootView;
        }

        OpusCodec exchangeOpusCodec(View rootView, boolean store) {

            Spinner app = rootView.findViewById(R.id.opus_appSpin);
            Spinner sr = rootView.findViewById(R.id.opus_samplerateSpin);
            Spinner audchan = rootView.findViewById(R.id.opus_audchanSpin);
            CheckBox dtx = rootView.findViewById(R.id.opus_dtxCheckBox);
            CheckBox vbr = rootView.findViewById(R.id.opus_vbrCheckBox);
            Spinner framesizeSpin = rootView.findViewById(R.id.opus_fsmsec_mode_spin);
            final SeekBar framesizeSeekBar = rootView.findViewById(R.id.opus_fsmsecSeekBar);
            final EditText framesizeEdit = rootView.findViewById(R.id.opus_fsmsec_edit);
            final TextView framesizeText = rootView.findViewById(R.id.opus_fsmsecTextView);

            Spinner txintervalSpin = rootView.findViewById(R.id.opus_txinterval_mode_spin);
            SeekBar txintervalSeekBar = rootView.findViewById(R.id.opus_txintervalSeekBar);
            final EditText txintervalEdit = rootView.findViewById(R.id.opus_txinterval_edit);
            final TextView txintervalText = rootView.findViewById(R.id.opus_txintervalTextView);

            Spinner bitrateSpin = rootView.findViewById(R.id.opus_bitrate_mode_spin);
            SeekBar bitrateSeekBar = rootView.findViewById(R.id.opus_bitrateSeekBar);
            final TextView bitrateText = rootView.findViewById(R.id.opus_brTextView);
            final EditText bitrateEdit = rootView.findViewById(R.id.opus_bitrate_edit);

            final CheckBox fixedVolume = rootView.findViewById(R.id.chan_fixed_volume);
            final View gainLayout = rootView.findViewById(R.id.chan_gain_level_layout);
            final SeekBar gainLevel = rootView.findViewById(R.id.chan_gain_level);
            final EditText gainLevelEdit = rootView.findViewById(R.id.chan_gain_level_edit);
            final TextView gainLevelValue = rootView.findViewById(R.id.chan_gain_level_value);

            fixedVolume.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    gainLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    audiocfg.bEnableAGC = isChecked;
                }
            });
            gainLayout.setVisibility(fixedVolume.isChecked() ? View.VISIBLE : View.GONE);

            if(store) {
                audiocfg.bEnableAGC = fixedVolume.isChecked();
                audiocfg.nGainLevel = gainLevel.getProgress();

                opuscodec.nApplication = appMap.getValue(app.getSelectedItemPosition(), 
                                                         OpusConstants.DEFAULT_OPUS_APPLICATION);
                opuscodec.nSampleRate = srMap.getValue(sr.getSelectedItemPosition(), 
                                                       OpusConstants.DEFAULT_OPUS_SAMPLERATE);
                opuscodec.nChannels = audMap.getValue(audchan.getSelectedItemPosition(),
                                                      OpusConstants.DEFAULT_OPUS_CHANNELS);
                opuscodec.nComplexity = OpusConstants.DEFAULT_OPUS_COMPLEXITY;
                opuscodec.bFEC = OpusConstants.DEFAULT_OPUS_FEC;
                opuscodec.bDTX = dtx.isChecked(); 
                opuscodec.bVBR = vbr.isChecked();
                opuscodec.bVBRConstraint = OpusConstants.DEFAULT_OPUS_VBRCONSTRAINT;
                
                opuscodec.bitrate_mode = bitrateSpin.getSelectedItemPosition();
                if(opuscodec.bitrate_mode == 0)
                    opuscodec.nBitRate = bitrateSeekBar.getProgress() * 1000 + OpusConstants.OPUS_MIN_BITRATE;
                else try {
                    opuscodec.nBitRate = Integer.parseInt(bitrateEdit.getText().toString()) * 1000;
                } catch(NumberFormatException e) { }

                opuscodec.txinterval_mode = txintervalSpin.getSelectedItemPosition();
                if(opuscodec.txinterval_mode == 0)
                    opuscodec.nTxIntervalMSec = txintervalSeekBar.getProgress() + TeamTalkConstants.OPUS_MIN_TXINTERVALMSEC;
                else try {
                    opuscodec.nTxIntervalMSec = Integer.parseInt(txintervalEdit.getText().toString());
                } catch(NumberFormatException e) { }
                
                opuscodec.fsmsec_mode = framesizeSpin.getSelectedItemPosition();
                if(opuscodec.fsmsec_mode == 0)
                    opuscodec.nFrameSizeMSec = fsMap.getValue(framesizeSeekBar.getProgress(), TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC);
                else try {
                    opuscodec.nFrameSizeMSec = Integer.parseInt(framesizeEdit.getText().toString());
                } catch(NumberFormatException e) { }
            }
            else {
                // 1. Initialize adapters and max values
                app.setAdapter(appMap);
                sr.setAdapter(srMap);
                audchan.setAdapter(audMap);
                bitrateSpin.setAdapter(bitrateModeMap);
                txintervalSpin.setAdapter(txintervalModeMap);
                framesizeSpin.setAdapter(fsmsecModeMap);

                int maxbr = OpusConstants.OPUS_MAX_BITRATE - OpusConstants.OPUS_MIN_BITRATE;
                bitrateSeekBar.setMax(maxbr / 1000);
                int maxtxinterval = TeamTalkConstants.OPUS_MAX_TXINTERVALMSEC - TeamTalkConstants.OPUS_MIN_TXINTERVALMSEC;
                txintervalSeekBar.setMax(maxtxinterval);
                framesizeSeekBar.setMax(fsMap.getCount() - 1);
                gainLevel.setMax(32000);

                // 2. Set initial values
                app.setSelection(appMap.getIndex(opuscodec.nApplication, 0));
                sr.setSelection(srMap.getIndex(opuscodec.nSampleRate, 1));
                audchan.setSelection(audMap.getIndex(opuscodec.nChannels, 0));
                vbr.setChecked(opuscodec.bVBR);
                dtx.setChecked(opuscodec.bDTX);

                bitrateSpin.setSelection(opuscodec.bitrate_mode);
                bitrateSeekBar.setProgress((opuscodec.nBitRate / 1000) - (OpusConstants.OPUS_MIN_BITRATE / 1000));
                bitrateEdit.setText(String.valueOf(opuscodec.nBitRate / 1000));
                bitrateText.setText(getString(R.string.fmt_label_value, String.valueOf(opuscodec.nBitRate / 1000), getString(R.string.unit_kbits)));
                
                txintervalSpin.setSelection(opuscodec.txinterval_mode);
                txintervalSeekBar.setProgress(opuscodec.nTxIntervalMSec - TeamTalkConstants.OPUS_MIN_TXINTERVALMSEC);
                txintervalEdit.setText(String.valueOf(opuscodec.nTxIntervalMSec));
                txintervalText.setText(getString(R.string.fmt_label_value, String.valueOf(opuscodec.nTxIntervalMSec), getString(R.string.unit_msec)));

                framesizeSpin.setSelection(opuscodec.fsmsec_mode);
                int fsIdx = fsMap.getIndex(opuscodec.nFrameSizeMSec, -1);
                int progress = fsIdx != -1 ? fsIdx : fsMap.getIndex(TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC, 0);
                framesizeSeekBar.setProgress(progress);
                framesizeEdit.setText(String.valueOf(opuscodec.nFrameSizeMSec));
                framesizeText.setText(getString(R.string.fmt_label_value, String.valueOf(opuscodec.nFrameSizeMSec), getString(R.string.unit_msec)));

                fixedVolume.setChecked(audiocfg.bEnableAGC);
                gainLevel.setProgress(audiocfg.nGainLevel);
                gainLevelValue.setText(String.valueOf(audiocfg.nGainLevel));
                gainLevelEdit.setText(String.valueOf(audiocfg.nGainLevel));

                bitrateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int br = progress + (OpusConstants.OPUS_MIN_BITRATE / 1000);
                        String val = getString(R.string.fmt_label_value, String.valueOf(br), getString(R.string.unit_kbits));
                        bitrateText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_bitrate) + ": " + val);
                        bitrateEdit.setText(String.valueOf(br));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                txintervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int interval = progress + TeamTalkConstants.OPUS_MIN_TXINTERVALMSEC;
                        String val = getString(R.string.fmt_label_value, String.valueOf(interval), getString(R.string.unit_msec));
                        txintervalText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_txinterval) + ": " + val);
                        txintervalEdit.setText(String.valueOf(interval));
                        
                        // framesize dependency
                        int selFramesize = fsMap.getValue(framesizeSeekBar.getProgress(), TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC);
                        if (interval > OpusConstants.OPUS_REALMAX_FRAMESIZE && selFramesize == TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC)
                            framesizeSeekBar.setProgress(fsMap.getIndex(OpusConstants.OPUS_REALMAX_FRAMESIZE, 0));
                        else if (selFramesize > interval)
                            framesizeSeekBar.setProgress(fsMap.getIndex(TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC, 0));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                framesizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int fs = fsMap.getValue(progress, TeamTalkConstants.OPUS_DEFAULT_FRAMESIZEMSEC);
                        String val = getString(R.string.fmt_label_value, String.valueOf(fs), getString(R.string.unit_msec));
                        framesizeText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_framesize) + ": " + val);
                        framesizeEdit.setText(String.valueOf(fs));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                gainLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        String val = String.valueOf(progress);
                        gainLevelValue.setText(val);
                        seekBar.setContentDescription(getString(R.string.channel_prop_title_gain_level) + ": " + val);
                        gainLevelEdit.setText(val);
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                final View fBitrate = bitrateSeekBar, fBitrateEdit = bitrateEdit, fBitrateText = bitrateText;
                final View fTxInterval = txintervalSeekBar, fTxIntervalEdit = txintervalEdit, fTxIntervalText = txintervalText;
                final SeekBar fFsSeekBar = framesizeSeekBar;
                final View fFsEdit = framesizeEdit, fFsText = framesizeText;
                final View fGain = gainLevel, fGainEdit = gainLevelEdit;

                final Spinner fBitrateSpin = bitrateSpin, fTxIntervalSpin = txintervalSpin, fFsSpin = framesizeSpin;

                rootView.post(new Runnable() {
                    @Override
                    public void run() {
                        fBitrateSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fBitrate.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fBitrateText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fBitrateEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });
                        
                        fTxIntervalSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fTxInterval.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });

                        fFsSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fFsSeekBar.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fFsText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fFsEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });
                        
                        fBitrate.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fBitrateText.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fBitrateEdit.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fTxInterval.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalText.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalEdit.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fFsSeekBar.setVisibility(fFsSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fFsText.setVisibility(fFsSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fFsEdit.setVisibility(fFsSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fGain.setVisibility(View.VISIBLE);
                        fGainEdit.setVisibility(View.GONE);
                        ((View)fBitrate.getParent()).requestLayout();
                    }
                });
            }
            return opuscodec;
        }
    }

    public static class SpeexFragment extends Fragment {

        SpeexCodec speexcodec;
        AudioConfig audiocfg;
        MapAdapter srMap;
        MapAdapter qualityModeMap, txintervalModeMap;

        public SpeexFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            speexcodec = ((AudioCodecActivity)activity).audiocodec.speex;
            audiocfg = ((AudioCodecActivity)activity).audiocfg;
            srMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            srMap.addPair(getString(R.string.bandmode_8khz), SpeexConstants.SPEEX_BANDMODE_NARROW);
            srMap.addPair(getString(R.string.bandmode_16khz), SpeexConstants.SPEEX_BANDMODE_WIDE);
            srMap.addPair(getString(R.string.bandmode_32khz), SpeexConstants.SPEEX_BANDMODE_UWIDE);

            qualityModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            qualityModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            qualityModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            txintervalModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_audiocodec_speex,
                                             container, false);
            ((AudioCodecActivity)getActivity()).accessibilityAssistant.registerPage(rootView, TAB_SPEEX);
            exchangeSpeexCodec(rootView, false);

            return rootView;
        }

        SpeexCodec exchangeSpeexCodec(View rootView, boolean store) {

            Spinner sr = rootView.findViewById(R.id.speex_bandmodeSpin);
            
            Spinner qualitySpin = rootView.findViewById(R.id.speex_quality_mode_spin);
            SeekBar quality = rootView.findViewById(R.id.speex_qualitySeekBar);
            final EditText qualityEdit = rootView.findViewById(R.id.speex_quality_edit);
            final TextView qualityText = rootView.findViewById(R.id.speex_quality_value);

            Spinner txintervalSpin = rootView.findViewById(R.id.speex_txinterval_mode_spin);
            SeekBar txinterval = rootView.findViewById(R.id.speex_txintervalSeekBar);
            final EditText txintervalEdit = rootView.findViewById(R.id.speex_txinterval_edit);
            final TextView txintervalText = rootView.findViewById(R.id.speex_txintervalTextView);
    
            final CheckBox fixedVolume = rootView.findViewById(R.id.chan_fixed_volume);
            final View gainLayout = rootView.findViewById(R.id.chan_gain_level_layout);
            final SeekBar gainLevel = rootView.findViewById(R.id.chan_gain_level);
            final EditText gainLevelEdit = rootView.findViewById(R.id.chan_gain_level_edit);
            final TextView gainLevelValue = rootView.findViewById(R.id.chan_gain_level_value);
    
            fixedVolume.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    gainLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    audiocfg.bEnableAGC = isChecked;
                }
            });
            gainLayout.setVisibility(fixedVolume.isChecked() ? View.VISIBLE : View.GONE);

            if(store) {
                audiocfg.bEnableAGC = fixedVolume.isChecked();
                audiocfg.nGainLevel = gainLevel.getProgress();

                speexcodec.nBandmode = srMap.getValue(sr.getSelectedItemPosition(), SpeexConstants.DEFAULT_SPEEX_BANDMODE);
                
                speexcodec.quality_mode = qualitySpin.getSelectedItemPosition();
                if(speexcodec.quality_mode == 0)
                    speexcodec.nQuality = quality.getProgress() + SpeexConstants.SPEEX_QUALITY_MIN;
                else try {
                    speexcodec.nQuality = Integer.parseInt(qualityEdit.getText().toString());
                } catch(NumberFormatException e) { }
                
                speexcodec.txinterval_mode = txintervalSpin.getSelectedItemPosition();
                if(speexcodec.txinterval_mode == 0)
                    speexcodec.nTxIntervalMSec = txinterval.getProgress() + TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                else try {
                    speexcodec.nTxIntervalMSec = Integer.parseInt(txintervalEdit.getText().toString());
                } catch(NumberFormatException e) { }
            }
            else {
                // 1. Initialize adapters and max values
                sr.setAdapter(srMap);
                qualitySpin.setAdapter(qualityModeMap);
                txintervalSpin.setAdapter(txintervalModeMap);
                
                quality.setMax(SpeexConstants.SPEEX_QUALITY_MAX - SpeexConstants.SPEEX_QUALITY_MIN);
                int maxtxinterval = TeamTalkConstants.SPEEX_MAX_TXINTERVALMSEC - TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                txinterval.setMax(maxtxinterval);
                gainLevel.setMax(32000);

                // 2. Set initial values
                sr.setSelection(srMap.getIndex(speexcodec.nBandmode, 1));
                
                qualitySpin.setSelection(speexcodec.quality_mode);
                quality.setProgress(speexcodec.nQuality - SpeexConstants.SPEEX_QUALITY_MIN);
                qualityEdit.setText(String.valueOf(speexcodec.nQuality));
                qualityText.setText(String.valueOf(speexcodec.nQuality));
                
                txintervalSpin.setSelection(speexcodec.txinterval_mode);
                txinterval.setProgress(speexcodec.nTxIntervalMSec - TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC);
                txintervalEdit.setText(String.valueOf(speexcodec.nTxIntervalMSec));
                txintervalText.setText(getString(R.string.fmt_label_value, String.valueOf(speexcodec.nTxIntervalMSec), getString(R.string.unit_msec)));

                fixedVolume.setChecked(audiocfg.bEnableAGC);
                gainLevel.setProgress(audiocfg.nGainLevel);
                gainLevelValue.setText(String.valueOf(audiocfg.nGainLevel));
                gainLevelEdit.setText(String.valueOf(audiocfg.nGainLevel));

                // 3. Attach listeners
                quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int q = progress + SpeexConstants.SPEEX_QUALITY_MIN;
                        qualityText.setText(String.valueOf(q));
                        seekBar.setContentDescription(getString(R.string.text_quality) + ": " + q);
                        qualityEdit.setText(String.valueOf(q));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                gainLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        String val = String.valueOf(progress);
                        gainLevelValue.setText(val);
                        seekBar.setContentDescription(getString(R.string.channel_prop_title_gain_level) + ": " + val);
                        gainLevelEdit.setText(val);
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                txinterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int interval = progress + TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                        String val = getString(R.string.fmt_label_value, String.valueOf(interval), getString(R.string.unit_msec));
                        txintervalText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_txinterval) + ": " + val);
                        txintervalEdit.setText(String.valueOf(interval));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                final View fQuality = quality, fQualityEdit = qualityEdit, fQualityText = qualityText;
                final View fTxInterval = txinterval, fTxIntervalEdit = txintervalEdit, fTxIntervalText = txintervalText;
                final View fGain = gainLevel, fGainEdit = gainLevelEdit;
                final Spinner fQualitySpin = qualitySpin, fTxIntervalSpin = txintervalSpin;

                rootView.post(new Runnable() {
                    @Override
                    public void run() {
                        fQualitySpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fQuality.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fQualityText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fQualityEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });
                        
                        fTxIntervalSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fTxInterval.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });

                        fQuality.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fQualityText.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fQualityEdit.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fTxInterval.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalText.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalEdit.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fGain.setVisibility(View.VISIBLE);
                        fGainEdit.setVisibility(View.GONE);
                        ((View)fQuality.getParent()).requestLayout();
                    }
                });
            }
            return speexcodec;
        }
    }

    public static class SpeexVBRFragment extends Fragment {

        SpeexVBRCodec speexvbrcodec;
        AudioConfig audiocfg;
        MapAdapter srMap;
        MapAdapter qualityModeMap, bitrateModeMap, txintervalModeMap;

        public SpeexVBRFragment() {
        }

        @Override
        public void onAttach(@NonNull Activity activity) {
            speexvbrcodec = ((AudioCodecActivity)activity).audiocodec.speex_vbr;
            audiocfg = ((AudioCodecActivity)activity).audiocfg;
            srMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            srMap.addPair(getString(R.string.bandmode_8khz), SpeexConstants.SPEEX_BANDMODE_NARROW);
            srMap.addPair(getString(R.string.bandmode_16khz), SpeexConstants.SPEEX_BANDMODE_WIDE);
            srMap.addPair(getString(R.string.bandmode_32khz), SpeexConstants.SPEEX_BANDMODE_UWIDE);

            qualityModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            qualityModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            qualityModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            bitrateModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            bitrateModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            bitrateModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            txintervalModeMap = new MapAdapter(activity, R.layout.item_spinner, R.id.spinTextView);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_slider), 0);
            txintervalModeMap.addPair(getString(R.string.bitrate_mode_custom), 1);

            super.onAttach(activity);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_audiocodec_speexvbr,
                                             container, false);
            ((AudioCodecActivity)getActivity()).accessibilityAssistant.registerPage(rootView, TAB_SPEEXVBR);
            exchangeSpeexVBRCodec(rootView, false);

            return rootView;
        }

        SpeexVBRCodec exchangeSpeexVBRCodec(View rootView, boolean store) {

            Spinner sr = rootView.findViewById(R.id.speexvbr_bandmodeSpin);
            CheckBox dtx = rootView.findViewById(R.id.speexvbr_dtxCheckBox);

            Spinner qualitySpin = rootView.findViewById(R.id.speexvbr_quality_mode_spin);
            SeekBar quality = rootView.findViewById(R.id.speexvbr_qualitySeekBar);
            final EditText qualityEdit = rootView.findViewById(R.id.speexvbr_quality_edit);
            final TextView qualityText = rootView.findViewById(R.id.speexvbr_quality_value);

            Spinner bitrateSpin = rootView.findViewById(R.id.speexvbr_bitrate_mode_spin);
            SeekBar bitrate = rootView.findViewById(R.id.speexvbr_bitrateSeekBar);
            final EditText bitrateEdit = rootView.findViewById(R.id.speexvbr_bitrate_edit);
            final TextView bitrateText = rootView.findViewById(R.id.speexvbr_bitrate_value);

            Spinner txintervalSpin = rootView.findViewById(R.id.speexvbr_txinterval_mode_spin);
            SeekBar txinterval = rootView.findViewById(R.id.speexvbr_txintervalSeekBar);
            final EditText txintervalEdit = rootView.findViewById(R.id.speexvbr_txinterval_edit);
            final TextView txintervalText = rootView.findViewById(R.id.speexvbr_txintervalTextView);

            final CheckBox fixedVolume = rootView.findViewById(R.id.chan_fixed_volume);
            final View gainLayout = rootView.findViewById(R.id.chan_gain_level_layout);
            final SeekBar gainLevel = rootView.findViewById(R.id.chan_gain_level);
            final EditText gainLevelEdit = rootView.findViewById(R.id.chan_gain_level_edit);
            final TextView gainLevelValue = rootView.findViewById(R.id.chan_gain_level_value);
    
            fixedVolume.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    gainLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    audiocfg.bEnableAGC = isChecked;
                }
            });
            gainLayout.setVisibility(fixedVolume.isChecked() ? View.VISIBLE : View.GONE);

            if(store) {
                audiocfg.bEnableAGC = fixedVolume.isChecked();
                audiocfg.nGainLevel = gainLevel.getProgress();

                speexvbrcodec.nBandmode = srMap.getValue(sr.getSelectedItemPosition(), SpeexConstants.DEFAULT_SPEEX_BANDMODE);
                speexvbrcodec.bDTX = dtx.isChecked();

                speexvbrcodec.quality_mode = qualitySpin.getSelectedItemPosition();
                if (speexvbrcodec.quality_mode == 0)
                    speexvbrcodec.nQuality = quality.getProgress() + SpeexConstants.SPEEX_QUALITY_MIN;
                else try {
                    speexvbrcodec.nQuality = Integer.parseInt(qualityEdit.getText().toString());
                } catch(NumberFormatException e) { }
                
                speexvbrcodec.bitrate_mode = bitrateSpin.getSelectedItemPosition();
                if (speexvbrcodec.bitrate_mode == 0) {
                    int bandmode = speexvbrcodec.nBandmode;
                    int minbr = getMinBitRate(bandmode);
                    speexvbrcodec.nMaxBitRate = (bitrate.getProgress() * 100) + minbr;
                }
                else try {
                    speexvbrcodec.nMaxBitRate = Integer.parseInt(bitrateEdit.getText().toString()) * 1000;
                } catch(NumberFormatException e) { }
                
                speexvbrcodec.txinterval_mode = txintervalSpin.getSelectedItemPosition();
                if (speexvbrcodec.txinterval_mode == 0)
                    speexvbrcodec.nTxIntervalMSec = txinterval.getProgress() + TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                else try {
                    speexvbrcodec.nTxIntervalMSec = Integer.parseInt(txintervalEdit.getText().toString());
                } catch(NumberFormatException e) { }
            }
            else {
                // 1. Initialize adapters and max values
                sr.setAdapter(srMap);
                qualitySpin.setAdapter(qualityModeMap);
                bitrateSpin.setAdapter(bitrateModeMap);
                txintervalSpin.setAdapter(txintervalModeMap);

                final int bandmode_init = speexvbrcodec.nBandmode;
                final int minbr_init = getMinBitRate(bandmode_init);
                final int maxbr_init = getMaxBitRate(bandmode_init);

                quality.setMax(SpeexConstants.SPEEX_QUALITY_MAX - SpeexConstants.SPEEX_QUALITY_MIN);
                bitrate.setMax((maxbr_init - minbr_init) / 100);
                int maxtxinterval = TeamTalkConstants.SPEEX_MAX_TXINTERVALMSEC - TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                txinterval.setMax(maxtxinterval);
                gainLevel.setMax(32000);

                sr.setSelection(srMap.getIndex(speexvbrcodec.nBandmode, 1));
                dtx.setChecked(speexvbrcodec.bDTX);
                
                qualitySpin.setSelection(speexvbrcodec.quality_mode);
                quality.setProgress(speexvbrcodec.nQuality - SpeexConstants.SPEEX_QUALITY_MIN);
                qualityEdit.setText(String.valueOf(speexvbrcodec.nQuality));
                qualityText.setText(String.valueOf(speexvbrcodec.nQuality));
                
                bitrateSpin.setSelection(speexvbrcodec.bitrate_mode);
                int currentBr = speexvbrcodec.nMaxBitRate > 0 ? speexvbrcodec.nMaxBitRate : maxbr_init;
                bitrate.setProgress((currentBr - minbr_init) / 100);
                bitrateEdit.setText(String.valueOf(currentBr / 1000));
                bitrateText.setText(getString(R.string.fmt_label_value, String.valueOf(currentBr / 1000), getString(R.string.unit_kbits)));
                
                txintervalSpin.setSelection(speexvbrcodec.txinterval_mode);
                txinterval.setProgress(speexvbrcodec.nTxIntervalMSec - TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC);
                txintervalEdit.setText(String.valueOf(speexvbrcodec.nTxIntervalMSec));
                txintervalText.setText(getString(R.string.fmt_label_value, String.valueOf(speexvbrcodec.nTxIntervalMSec), getString(R.string.unit_msec)));

                fixedVolume.setChecked(audiocfg.bEnableAGC);
                gainLevel.setProgress(audiocfg.nGainLevel);
                gainLevelValue.setText(String.valueOf(audiocfg.nGainLevel));
                gainLevelEdit.setText(String.valueOf(audiocfg.nGainLevel));

                // 3. Attach listeners
                quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int q = progress + SpeexConstants.SPEEX_QUALITY_MIN;
                        qualityText.setText(String.valueOf(q));
                        seekBar.setContentDescription(getString(R.string.text_quality) + ": " + q);
                        qualityEdit.setText(String.valueOf(q));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int bandmode = srMap.getValue(sr.getSelectedItemPosition(), SpeexConstants.DEFAULT_SPEEX_BANDMODE);
                        int minbr = getMinBitRate(bandmode);
                        int br = ((progress * 100) + minbr) / 1000;
                        String val = getString(R.string.fmt_label_value, String.valueOf(br), getString(R.string.unit_kbits));
                        bitrateText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_bitrate) + ": " + val);
                        bitrateEdit.setText(String.valueOf(br));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                sr.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        int bandmode = srMap.getValue(position, SpeexConstants.DEFAULT_SPEEX_BANDMODE);
                        int minbr = getMinBitRate(bandmode);
                        int maxbr = getMaxBitRate(bandmode);
                        bitrate.setMax((maxbr - minbr) / 100);
                        
                        // Sync current value or reset to max if out of bounds
                        int currentKbps;
                        try {
                            currentKbps = Integer.parseInt(bitrateEdit.getText().toString());
                        } catch(Exception e) { currentKbps = maxbr / 1000; }
                        
                        int currentBr = currentKbps * 1000;
                        if (currentBr < minbr) currentBr = minbr;
                        if (currentBr > maxbr) currentBr = maxbr;
                        
                        bitrate.setProgress((currentBr - minbr) / 100);
                        String val = getString(R.string.fmt_label_value, String.valueOf(currentBr / 1000), getString(R.string.unit_kbits));
                        bitrateText.setText(val);
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });

                txinterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int interval = progress + TeamTalkConstants.SPEEX_MIN_TXINTERVALMSEC;
                        String val = getString(R.string.fmt_label_value, String.valueOf(interval), getString(R.string.unit_msec));
                        txintervalText.setText(val);
                        seekBar.setContentDescription(getString(R.string.text_txinterval) + ": " + val);
                        txintervalEdit.setText(String.valueOf(interval));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                gainLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        String val = String.valueOf(progress);
                        gainLevelValue.setText(val);
                        seekBar.setContentDescription(getString(R.string.channel_prop_title_gain_level) + ": " + val);
                        gainLevelEdit.setText(val);
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                final View fQuality = quality, fQualityEdit = qualityEdit, fQualityText = qualityText;
                final View fBitrate = bitrate, fBitrateEdit = bitrateEdit, fBitrateText = bitrateText;
                final View fTxInterval = txinterval, fTxIntervalEdit = txintervalEdit, fTxIntervalText = txintervalText;
                final View fGain = gainLevel, fGainEdit = gainLevelEdit;

                final Spinner fQualitySpin = qualitySpin, fBitrateSpin = bitrateSpin, fTxIntervalSpin = txintervalSpin;

                rootView.post(new Runnable() {
                    @Override
                    public void run() {
                        fQualitySpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fQuality.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fQualityText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fQualityEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });

                        fBitrateSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fBitrate.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fBitrateText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fBitrateEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });
                        
                        fTxIntervalSpin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                boolean custom = position == 1;
                                fTxInterval.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalText.setVisibility(custom ? View.GONE : View.VISIBLE);
                                fTxIntervalEdit.setVisibility(custom ? View.VISIBLE : View.GONE);
                            }
                            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });

                        fQuality.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fQualityText.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fQualityEdit.setVisibility(fQualitySpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fBitrate.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fBitrateText.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fBitrateEdit.setVisibility(fBitrateSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fTxInterval.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalText.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.GONE : View.VISIBLE);
                        fTxIntervalEdit.setVisibility(fTxIntervalSpin.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);

                        fGain.setVisibility(View.VISIBLE);
                        fGainEdit.setVisibility(View.GONE);
                        ((View)fQuality.getParent()).requestLayout();
                    }
                });
            }
            return speexvbrcodec;
        }

        private int progressToBitrate(int progress, int minbr) {
            return progress * 100 + minbr;
        }

        private int getMinBitRate(int bandmode) {
            switch(bandmode) {
                case SpeexConstants.SPEEX_BANDMODE_NARROW:
                    return SpeexConstants.SPEEX_NB_MIN_BITRATE;
                case SpeexConstants.SPEEX_BANDMODE_WIDE:
                    return SpeexConstants.SPEEX_WB_MIN_BITRATE;
                case SpeexConstants.SPEEX_BANDMODE_UWIDE:
                    return SpeexConstants.SPEEX_UWB_MIN_BITRATE;
                default:
                    return SpeexConstants.SPEEX_WB_MIN_BITRATE;
            }
        }

        private int getMaxBitRate(int bandmode) {
            switch(bandmode) {
                case SpeexConstants.SPEEX_BANDMODE_NARROW:
                    return SpeexConstants.SPEEX_NB_MAX_BITRATE;
                case SpeexConstants.SPEEX_BANDMODE_WIDE:
                    return SpeexConstants.SPEEX_WB_MAX_BITRATE;
                case SpeexConstants.SPEEX_BANDMODE_UWIDE:
                    return SpeexConstants.SPEEX_UWB_MAX_BITRATE;
                default:
                    return SpeexConstants.SPEEX_WB_MAX_BITRATE;
            }
        }
    }

    public static class NoAudioFragment extends Fragment {

        public NoAudioFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_audiocodec_noaudio,
                                             container, false);
        }
    }

}