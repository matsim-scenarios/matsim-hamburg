package org.matsim.analysis.pt;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author zmeng
 */
public class PtValidatorConfigGroup extends ReflectiveConfigGroup {

    private static final String GROUP_NAME = "PtValidatorConfigGroup";

    private static final String SURVEY_COUNTS_DIRECTORY = "surveyCountsDirectory";

    private String SurveyCountsDirectory;

    @StringGetter(SURVEY_COUNTS_DIRECTORY)
    public String getSurveyCountsDirectory() {
        return SurveyCountsDirectory;
    }
    @StringSetter(SURVEY_COUNTS_DIRECTORY)
    public void setSurveyCountsDirectory(String surveyCountsDirectory) {
        SurveyCountsDirectory = surveyCountsDirectory;
    }

    public PtValidatorConfigGroup() {
        super(GROUP_NAME);
    }
}
