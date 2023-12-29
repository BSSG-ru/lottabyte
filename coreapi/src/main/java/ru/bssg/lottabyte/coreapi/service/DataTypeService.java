package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.datatype.FlatDataType;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.DataTypeRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataTypeService {
    private final DataTypeRepository dataTypeRepository;
    private final ArtifactType serviceArtifactType = ArtifactType.datatype;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("id", SearchColumn.ColumnType.UUID),
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp)
    };

    private final SearchColumnForJoin[] joinColumns = {};

    public DataType getDataTypeById(String dataTypeId, UserDetails userDetails)
            throws LottabyteException {
        DataType dataType = dataTypeRepository.getById(dataTypeId, userDetails);
        if (dataType == null)
            throw new LottabyteException(Message.LBE02501,
                            userDetails.getLanguage(), dataTypeId);

        return dataType;
    }

    public SearchResponse<FlatDataType> searchDataType(SearchRequestWithJoin request,
                                                                     UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatDataType> res = dataTypeRepository.searchDataType(request,
                searchableColumns, joinColumns, userDetails);
        return res;
    }
}
