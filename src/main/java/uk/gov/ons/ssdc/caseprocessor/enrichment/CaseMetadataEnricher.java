package uk.gov.ons.ssdc.caseprocessor.enrichment;

import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.common.model.entity.Case;

@Component
public class CaseMetadataEnricher {
  // function called after each case update,

  //    Discuss for doing this here in JAVA
  //    1.  No more services, less overhead.
  //    2.  No emitting events and reprocessing, then emitting new events (could annoy RH etc, event ordering)
  //    3.  Nice working JPA/Hibernate
  //    4.  Would need potentially rigid models for Metadata? Or maybe not.  I mean once we for example
  //    understand that it's an CIS survery.  We have fields like ScheduleOfNextRuns, which would be a list of
  //    when to run next, what to run, Hasrun
  //    Have a nice Interface, and then have a nice class for each 'type' of survey.
  //    Hopefully generic like: SurveyScheduling type:  IndividualCaseSchedule, Bulk schedule.
  //    Personally I suspect without a big political change we'll be bespoking for many surveys.

  public void updateCaseMetaData(Case caze) {
//    Map<String, String> metaData = caze.getMetaData();
//    Work our Enriching class to call from here.

     CaseMetaDataGeneric caseMetaDataGeneric = ObjectMappper(caze.getMetaData());

     CaseMetaDataUpdater caseMetaDataUpdater = null;

    // Yes make ENUMS in a switch,  hope against hope we could reuse the code for other surveys
     if( caseMetaDataGeneric.getMetaDataType().equals("IndividualCaseSchedule")) {
          caseMetaDataUpdater = new IndividualCaseScheduling();
     }

    // Perhaps return true if updated?
     if(caseMetaDataUpdater.updateCaseScheduling(caze)) {
        //EventLog here? decision to update the Case?  reasoning, new/old values
     }

//     Having a huge great list of scheduled tasks on each case, then scanning with ActionScheduler every X Mins,
//      seems unsustainable.  Could we when creating/altering scheduled tasks add them to a table a bit like
//      Fulfilments to process - if they were to be processed before Midnight?
//      Then everynight run a general job that moved 'jobs' there for the day? Or would it be a daily run anyway?
//      Maybe this is premature optimisation?


     return;
     }



    //  Arguments for doing it in Python/NodeJS/GO in a new service
//    1.  They play nicely with JSON.  We could have relatively fast changing JSON models. Python fine with this
//    2.  Event handling wise, we could either just receive the same event as everyone else, and emit back to RM
//        an event that just updates MetaData, and then explicitly that doesn't send another update event (else circular).
//        OR
//        It could sit in between RM and external queues.  Though if we 'enrich' here and then send back to CaseProcessor and RH,
//        we're not quite reflecting what's actually in RM?  Do we care?
//    3.
//
//}
