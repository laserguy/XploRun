package com.xplorun.model;

import com.xplorun.TTSService;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

@Entity
public class Settings {

    @Id(assignable = true)
    long id;
    public boolean isVoiceEnabled;
    public String language;
    @Convert(converter = VoiceOfConverter.class, dbType = String.class)
    public TTSService.VoiceOf voice;

    public Settings() {
    }

    public Settings(long id) {
        this.id = id;
    }

    public static class VoiceOfConverter implements PropertyConverter<TTSService.VoiceOf, String> {

        @Override
        public TTSService.VoiceOf convertToEntityProperty(String databaseValue) {
            return TTSService.VoiceOf.valueOf(databaseValue);
        }

        @Override
        public String convertToDatabaseValue(TTSService.VoiceOf entityProperty) {
            return entityProperty.toString();
        }
    }
}
