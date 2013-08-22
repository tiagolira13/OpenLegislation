package gov.nysenate.openleg.api;

import gov.nysenate.openleg.api.QueryBuilder.QueryBuilderException;
import gov.nysenate.openleg.api.SingleViewRequest.SingleView;
import gov.nysenate.openleg.model.Action;
import gov.nysenate.openleg.model.Bill;
import gov.nysenate.openleg.model.Calendar;
import gov.nysenate.openleg.model.IBaseObject;
import gov.nysenate.openleg.model.Meeting;
import gov.nysenate.openleg.model.Vote;
import gov.nysenate.openleg.util.Application;
import gov.nysenate.openleg.util.TextFormatter;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class SingleViewRequest1 extends AbstractApiRequest {
    private final Logger logger = Logger.getLogger(SingleViewRequest.class);

    String type;
    String id;

    public SingleViewRequest1(HttpServletRequest request, HttpServletResponse response,
            String format, String type, String id, int pageIdx, int pageSize, String sort, boolean sortOrder) {
        super(request, response, 1, 1, format, getApiEnum(SingleView.values(),type));
        logger.info("New single view request: format="+format+", type="+type+", id="+id);
        this.type = type;
        this.id = id;
    }

    @Override
    public void fillRequest() throws ApiRequestException {
        IBaseObject so = Application.getLucene().getSenateObject(id, type);

        if(so == null) {
            throw new ApiRequestException(TextFormatter.append("couldn't find id: ", id, " of type: ", type));
        }

        request.setAttribute(type , so);

        try {
            if(type.equals("bill") && !format.matches("(csv|json|xml)")) {
                String rType = "action";
                String rQuery = QueryBuilder.build().otype(rType).and().relatedBills("billno", id).query();
                ArrayList<Action> billEvents = Application.getLucene().getSenateObjects(rQuery);
                request.setAttribute("related-" + rType, billEvents);

                rType = "bill";
                rQuery = QueryBuilder.build().otype(rType).and().relatedBills("oid", id).query();
                ArrayList<Bill> bills = Application.getLucene().getSenateObjects(rQuery);
                request.setAttribute("related-" + rType, bills);

                rType = "meeting";
                rQuery = QueryBuilder.build().otype(rType).and().keyValue("bills", id).query();
                ArrayList<Meeting> meetings = Application.getLucene().getSenateObjects(rQuery);
                request.setAttribute("related-" + rType, meetings);

                rType = "calendar";
                rQuery = QueryBuilder.build().otype(rType).and().keyValue("bills", id).query();
                ArrayList<Calendar> calendars = Application.getLucene().getSenateObjects(rQuery);
                request.setAttribute("related-" + rType, calendars);

                rType = "vote";
                rQuery = QueryBuilder.build().otype(rType).and().relatedBills("billno", id).query();
                ArrayList<Vote> votes = Application.getLucene().getSenateObjects(rQuery);
                request.setAttribute("related-" + rType, votes);
            }
        } catch (QueryBuilderException e) {
            logger.error(e);
        }
    }

    @Override
    public String getView() {
        String vFormat = format.equals("jsonp") ? "json" : format;
        return TextFormatter.append("/views/", type, "-", vFormat, ".jsp");
    }

    @Override
    public boolean hasParameters() {
        return type != null && id != null;
    }
}
