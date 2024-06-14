package com.xplorun;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

public abstract class TTSService {
    public enum VoiceOf {
        Davis("en-US-DavisNeural"),
        Jenny("en-US-JennyNeural");
        private final String str;

        VoiceOf(String s) {
            this.str = s;
        }

        @NonNull
        @Override
        public String toString() {
            return str;
        }
    }

    public enum Style {
        Default("default"),
        Chat("chat"),
        Angry("angry"),
        Cheerful("cheerful"),
        Excited("excited"),
        Friendly("friendly"),
        Hopeful("hopeful"),
        Sad("sad"),
        Shouting("shouting"),
        Terrified("terrified"),
        Unfriendly("unfriendly"),
        Whispering("whispering");

        private final String str;

        Style(String s) {
            this.str = s;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    abstract void speak(String text, Style style);

    abstract void speak(String text);

    abstract void speakNext(String text);

    abstract void shutdown();

    abstract void setVoice(Voice voice);

    abstract Set<Voice> getVoices();

    private static TTSService instance;

    static TTSService getInstance(Context context) {
        if (instance == null) {
            instance = new TTSProviderImpl(context);
        }
        return instance;
    }

    static class TTSProviderImpl extends TTSService implements TextToSpeech.OnInitListener {
        private final String TAG = this.getClass().getSimpleName();
        private final Context context;
        private final TextToSpeech tts;
        private final SpeechConfig speechConfig;
        private final SpeechSynthesizer synth;
        private final VoiceOf defaultVoice;

        public TTSProviderImpl(Context context) {
            this.context = context;
            this.tts = new TextToSpeech(context, this);
            this.speechConfig = SpeechConfig.fromSubscription(BuildConfig.AZURE_TTS_KEY, BuildConfig.AZURE_TTS_REGION);
            this.synth = new SpeechSynthesizer(speechConfig);
            this.defaultVoice = VoiceOf.Davis;
        }

        private void setDefaultVoice() {
            Voice voice = null;
            for (Voice tmpVoice : getVoices()) {
                if (tmpVoice.getName().startsWith("en-us")) {
                    Log.i(TAG, tmpVoice.getName());
                }
                if (tmpVoice.getName().contains("en-us-x-iom-network")) {
                    voice = tmpVoice;
//                    break;
                }
            }
            if (voice != null) {
                setVoice(voice);
            }
        }

        private String ssml(String text, Style style, VoiceOf voice) {
            return MessageFormat.format(context.getString(R.string.ssml), voice, style, text);
        }

        private String ssml(String text, Style style) {
            return ssml(text, style, defaultVoice);
        }

        private String ssml(String text) {
            return ssml(text, Style.Default);
        }

        @Override
        public void speak(String text, Style style) {
            var result = synth.SpeakSsml(ssml(text, style));
            if (result.getReason() == ResultReason.Canceled) {
                handleCancelled(text, result);
            }
        }

        @Override
        public void speak(String text) {
            var result = synth.SpeakSsml(ssml(text));
            if (result.getReason() == ResultReason.Canceled) {
                handleCancelled(text, result);
            }
        }

        private void handleCancelled(String text, SpeechSynthesisResult result) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            var reason = SpeechSynthesisCancellationDetails.fromResult(result);
            Log.e(TAG, "Cancellation details: " + reason);
        }

        public void shutdown() {
            tts.shutdown();
            synth.close();
            speechConfig.close();
        }

        @Override
        public void speakNext(String text) {
            var result = synth.SpeakSsml(ssml(text));
            if (result.getReason() == ResultReason.Canceled) {
                handleCancelled(text, result);
            }
        }

        @Override
        public void setVoice(Voice voice) {
            tts.setVoice(voice);
        }

        @Override
        public Set<Voice> getVoices() {
            return tts.getVoices();
        }

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                setDefaultVoice();
//                speak("Initialized?", Style.Whispering);
            }
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(context, "TTS init failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
