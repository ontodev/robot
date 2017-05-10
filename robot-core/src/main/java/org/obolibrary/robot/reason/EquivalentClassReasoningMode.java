package org.obolibrary.robot.reason;

import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 * Created by edouglass on 5/5/17.
 *
 */
public enum EquivalentClassReasoningMode {

    /**
     * Allows all equivalent classes in the reason step. This is the default
     */
    ALL("All equivalent class axioms are allowed", "true"),
    /**
     * Does not allow any detected equivalent classes.
     */
    NONE("No equivalent class axioms are allowed", "false"),
    /**
     * Only allows directly asserted equivalent classes. If there are equivalent classes inferred that do are not
     * already asserted in the ontology, then this is considered an error.
     */
    ASSERTED_ONLY("Only equivalent classes that have been asserted are allowed. Inferred equivalencies are forbidden.");

    private static EquivalentClassReasoningMode DEFAULT = ALL;

    private List<String> synonyms;

    private String explanation;

    EquivalentClassReasoningMode(String explanation, String... synonyms) {
        this.synonyms = Lists.newArrayList(synonyms);
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }

    /**
     * The command-line written form of the mode
     */
    public String written() {
        return name().replace('_', '-').toLowerCase();
    }

    /**
     * Checks if the given string counts as this mode. A string validates to a mode if s is the written form or
     * one of the synonyms of this Mode.
     * @return True if the given string s is valid, False if not.
     */
    public boolean validates(String s) {
        return s.equals(this.written()) || synonyms.contains(s);
    }

    public static EquivalentClassReasoningMode from(String s) {
        for(EquivalentClassReasoningMode mode : EquivalentClassReasoningMode.values()) {
            if(mode.validates(s)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
