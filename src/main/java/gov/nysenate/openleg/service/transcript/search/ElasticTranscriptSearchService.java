package gov.nysenate.openleg.service.transcript.search;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.SearchIndex;
import gov.nysenate.openleg.dao.base.SortOrder;
import gov.nysenate.openleg.dao.transcript.search.ElasticTranscriptSearchDao;
import gov.nysenate.openleg.model.base.Environment;
import gov.nysenate.openleg.model.search.RebuildIndexEvent;
import gov.nysenate.openleg.model.search.SearchException;
import gov.nysenate.openleg.model.search.SearchResults;
import gov.nysenate.openleg.model.transcript.Transcript;
import gov.nysenate.openleg.model.transcript.TranscriptId;
import gov.nysenate.openleg.service.base.search.IndexedSearchService;
import gov.nysenate.openleg.service.transcript.data.TranscriptDataService;
import gov.nysenate.openleg.service.transcript.event.BulkTranscriptUpdateEvent;
import gov.nysenate.openleg.service.transcript.event.TranscriptUpdateEvent;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElasticTranscriptSearchService implements TranscriptSearchService, IndexedSearchService<Transcript> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticTranscriptSearchService.class);

    @Autowired protected Environment env;
    @Autowired protected EventBus eventBus;
    @Autowired protected ElasticTranscriptSearchDao transcriptSearchDao;
    @Autowired protected TranscriptDataService transcriptDataService;

    @PostConstruct
    protected void init() {
        eventBus.register(this);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResults<TranscriptId> searchTranscripts(String query, String sort, LimitOffset limOff) throws SearchException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SearchResults<TranscriptId> searchTranscripts(String query, int year, String sort, LimitOffset limOff) throws SearchException {
        PrefixFilterBuilder yearFilter = FilterBuilders.prefixFilter("dateTime", Integer.toString(year));
        return searchTranscripts(QueryBuilders.filteredQuery(QueryBuilders.queryString(query), yearFilter), null, sort, limOff);
    }

    private SearchResults<TranscriptId> searchTranscripts(QueryBuilder query, FilterBuilder postFilter, String sort, LimitOffset limOff)
        throws SearchException {
        if (limOff == null) limOff = LimitOffset.TEN;
        try {
            return transcriptSearchDao.searchTranscripts(query, postFilter, sort, limOff);
        }
        catch (SearchParseException ex) {
            throw new SearchException("Invalid query string", ex);
        }
        catch (ElasticsearchException ex) {
            throw new SearchException("Unexpected search exception!", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Subscribe
    public void handleTranscriptUpdate(TranscriptUpdateEvent transcriptUpdateEvent) {
        if (transcriptUpdateEvent.getTranscript() != null) {
            updateIndex(transcriptUpdateEvent.getTranscript());
        }
    }

    /** {@inheritDoc} */
    @Override
    @Subscribe
    public void handleBulkTranscriptUpdate(BulkTranscriptUpdateEvent bulkTranscriptUpdateEvent) {
        if (bulkTranscriptUpdateEvent.getTranscripts() != null) {
            updateIndex(bulkTranscriptUpdateEvent.getTranscripts());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateIndex(Transcript transcript) {
        if (env.isElasticIndexing() && transcript != null) {
            logger.info("Indexing transcript {} into elastic search.", transcript.getDateTime().toString());
            transcriptSearchDao.updateTranscriptIndex(transcript);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateIndex(Collection<Transcript> transcripts) {
        if (env.isElasticIndexing() && !transcripts.isEmpty()) {
            List<Transcript> indexableTranscripts = transcripts.stream().filter(t -> t != null).collect(Collectors.toList());
            logger.info("Indexing {} valid transcripts into elastic search.", indexableTranscripts.size());
            transcriptSearchDao.updateTranscriptIndex(indexableTranscripts);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clearIndex() {
        transcriptSearchDao.purgeIndices();
        transcriptSearchDao.createIndices();
    }

    /** {@inheritDoc} */
    @Override
    public void rebuildIndex() {
        clearIndex();
        for (int year = 1993; year <= LocalDate.now().getYear(); year++) {
            LimitOffset limOff = LimitOffset.TWENTY_FIVE;
            List<TranscriptId> transcriptIds = transcriptDataService.getTranscriptIds(SortOrder.DESC, limOff);
            while (!transcriptIds.isEmpty()) {
                logger.info("Indexing {} transcripts starting from {}", transcriptIds.size(), year);
                List<Transcript> transcripts = transcriptIds.stream().map(transcriptDataService::getTranscript).collect(Collectors.toList());
                updateIndex(transcripts);
                limOff = limOff.next();
                transcriptIds = transcriptDataService.getTranscriptIds(SortOrder.DESC, limOff);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleRebuildEvent(RebuildIndexEvent event) {
        if (event.affects(SearchIndex.TRANSCRIPT)) {
            logger.info("Handling transcript re-index event.");
            try {
                rebuildIndex();
            }
            catch (Exception ex) {
                logger.error("Unexpected exception during handling of transcript index rebuild event.", ex);
            }
        }
    }
}
