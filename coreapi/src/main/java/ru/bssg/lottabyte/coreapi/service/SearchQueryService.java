package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.SearchQueryRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchQueryService {
    private final SearchQueryRepository searchQueryRepository;

    public List<String> getPopularQueries(String search, Integer count, UserDetails userDetails) {
        return searchQueryRepository.getPopularQueries(search, count, userDetails);
    }

    public List<String> getUserQueries(Integer count, UserDetails userDetails) {
        return searchQueryRepository.getUserQueries(count, userDetails);
    }

    public Boolean saveQuery(String query, UserDetails userDetails) {
        searchQueryRepository.saveQuery(query, userDetails);
        return true;
    }
}
