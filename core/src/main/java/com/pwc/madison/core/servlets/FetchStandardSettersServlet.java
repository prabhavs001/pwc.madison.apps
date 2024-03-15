package com.pwc.madison.core.servlets;

import com.google.gson.Gson;
import com.pwc.madison.core.models.StandardSetterBean;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet to fetch the Standard setter folder names configured in OSGI to determine whether
 *  to show/hide the full-cycle or simple review workflow buttons for a selected asset.
 */
@Component(
    immediate = true,
    service = Servlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        "service.description=Fetch Standard Setter Names servlet",
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/pwc-madison/fetchSetters",
        "sling.servlet.extensions=json"
    }
)
@Designate(ocd = FetchStandardSettersServlet.Configuration.class)
public class FetchStandardSettersServlet extends SlingSafeMethodsServlet {

    String[] fullCycleWorkflowSetters, simpleWorkflowSetters;

    @Activate
    protected void Activate(Configuration config) {
        fullCycleWorkflowSetters = config.fullcycle_reviewtask_standardsetters();
        simpleWorkflowSetters = config.simple_reviewtask_standardsetters();
    }

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        StandardSetterBean standardSetters = new StandardSetterBean();

        standardSetters.setFullCycleWorkflowSetters(getStandardSetterPathRegex(fullCycleWorkflowSetters));

        standardSetters.setSimpleWorkflowSetters(getStandardSetterPathRegex(simpleWorkflowSetters));

        Gson gson = new Gson();
        gson.toJson(standardSetters, response.getWriter());

    }

    private List<String> getStandardSetterPathRegex(String[] standardSetters) {
        List<String> standardSetterList = new ArrayList<String>();
        for (String setterName : standardSetters) {
            standardSetterList.add("\\/content\\/dam\\/pwc-madison\\/ditaroot\\/.*\\/.*\\/" + setterName + "\\/.*");
        }
        return standardSetterList;
    }

    @ObjectClassDefinition(name = "PwC Viewpoint - Review workflows standard setters")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Exclude list for Full Cycle Review button",
            description = "The standard setter folders under which \"Create full cycle workflow\" button is to be hidden",
            type = AttributeType.STRING
        )
        String[] fullcycle_reviewtask_standardsetters() default {"fasb", "aicpa"};

        @AttributeDefinition(
            name = "Exclude list for Simple Review button",
            description = "The standard setter folders under which \"Create simplified workflow\" button is to be hidden",
            type = AttributeType.STRING
        )
        String[] simple_reviewtask_standardsetters() default {};
    }
}
