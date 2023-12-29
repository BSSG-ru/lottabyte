package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.ca.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.CustomAttributeDefinitionRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomAttributeDefinitionService {

    private final CustomAttributeDefinitionRepository customAttributeDefinitionRepository;

    // Definitions

    public CustomAttributeDefinition getCustomAttributeDefinitionById(String customAttributeDefinitionId, UserDetails userDetails) throws LottabyteException {
        CustomAttributeDefinition customAttributeDefinition = customAttributeDefinitionRepository.getById(customAttributeDefinitionId, userDetails);
        if (customAttributeDefinition.getEntity().getType().equals(AttributeType.Enumerated))
            customAttributeDefinition.getEntity().setDefElements(
                    customAttributeDefinitionRepository.getAllCustomAttributeDefElementPaginated(customAttributeDefinition.getId(), 0, 10000, userDetails).getResources()
                .stream().map(x -> new CustomAttributeEnumValue(x.getId(), x.getEntity().getName(), x.getEntity().getDescription()))
                .collect(Collectors.toList()));
        return customAttributeDefinition;
    }

    public PaginatedArtifactList<CustomAttributeDefinition> getAllCustomAttributeDefinitionPaginated(Integer offset, Integer limit,
                                                                                                     String artifactType, UserDetails userDetails) {
        PaginatedArtifactList<CustomAttributeDefinition> attributeDefinitionPaginatedArtifactList = customAttributeDefinitionRepository.getAllCustomAttributeDefinitionPaginated(artifactType, offset, limit, userDetails.getTenant());
        attributeDefinitionPaginatedArtifactList.getResources().stream().filter(x -> x.getEntity().getType().equals(AttributeType.Enumerated))
                .forEach(x -> x.getEntity().setDefElements(
                        customAttributeDefinitionRepository.getAllCustomAttributeDefElementPaginated(x.getId(), 0, 10000, userDetails).getResources()
                        .stream().map(y -> new CustomAttributeEnumValue(y.getId(), y.getEntity().getName(), y.getEntity().getDescription()))
                        .collect(Collectors.toList())));
        return attributeDefinitionPaginatedArtifactList;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public CustomAttributeDefinition createCustomAttributeDefinition(UpdatableCustomAttributeDefinitionEntity newCustomAttributeDefinitionEntity, UserDetails userDetails) throws LottabyteException {
        if (newCustomAttributeDefinitionEntity.getName() == null || newCustomAttributeDefinitionEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01810, userDetails.getLanguage());
        if (newCustomAttributeDefinitionEntity.getType() == null)
            throw new LottabyteException(Message.LBE01811, userDetails.getLanguage());
        if (newCustomAttributeDefinitionEntity.getMultipleValues() == null)
            throw new LottabyteException(Message.LBE01812, userDetails.getLanguage());
        if (newCustomAttributeDefinitionEntity.getRequired() == null)
            throw new LottabyteException(Message.LBE01815, userDetails.getLanguage());
        if (!newCustomAttributeDefinitionEntity.getType().equals(AttributeType.Numeric) &&
                (newCustomAttributeDefinitionEntity.getMaximum() != null || newCustomAttributeDefinitionEntity.getMinimum() != null))
            throw new LottabyteException(Message.LBE01813, userDetails.getLanguage());
        if (!newCustomAttributeDefinitionEntity.getType().equals(AttributeType.String) &&
                (newCustomAttributeDefinitionEntity.getMinLength() != null || newCustomAttributeDefinitionEntity.getMaxLength() != null))
            throw new LottabyteException(Message.LBE01814, userDetails.getLanguage());
        if (newCustomAttributeDefinitionEntity.getType().equals(AttributeType.Enumerated) &&
                (newCustomAttributeDefinitionEntity.getDefElements() == null || newCustomAttributeDefinitionEntity.getDefElements().isEmpty()))
            throw new LottabyteException(Message.LBE01816, userDetails.getLanguage());
        validateCustomAttributeDefinitionDefElements(newCustomAttributeDefinitionEntity, userDetails);
        String customAttributeDefinitionId = customAttributeDefinitionRepository.createCustomAttributeDefinition(newCustomAttributeDefinitionEntity, userDetails);
        if (newCustomAttributeDefinitionEntity.getType().equals(AttributeType.Enumerated)) {
            newCustomAttributeDefinitionEntity.getDefElements().forEach(x ->
                    customAttributeDefinitionRepository.createCustomAttributeDefElement(x, customAttributeDefinitionId, userDetails));
        }
        return getCustomAttributeDefinitionById(customAttributeDefinitionId, userDetails);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public CustomAttributeDefinition patchCustomAttributeDefinition(String customAttributeDefinitionId, UpdatableCustomAttributeDefinitionEntity customAttributeDefinitionEntity, UserDetails userDetails) throws LottabyteException {
        CustomAttributeDefinition current = getCustomAttributeDefinitionById(customAttributeDefinitionId, userDetails);
        if (current == null)
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), customAttributeDefinitionId);
        if (customAttributeDefinitionEntity.getName() != null && customAttributeDefinitionEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01810, userDetails.getLanguage());
        if (customAttributeDefinitionEntity.getType() != null && !customAttributeDefinitionEntity.getType().equals(current.getEntity().getType()))
            throw new LottabyteException(Message.LBE01808, userDetails.getLanguage());
        if (!current.getEntity().getType().equals(AttributeType.Numeric) &&
                (customAttributeDefinitionEntity.getMaximum() != null || customAttributeDefinitionEntity.getMinimum() != null))
            throw new LottabyteException(Message.LBE01813, userDetails.getLanguage());
        if (!current.getEntity().getType().equals(AttributeType.String) &&
                (customAttributeDefinitionEntity.getMinLength() != null || customAttributeDefinitionEntity.getMaxLength() != null))
            throw new LottabyteException(Message.LBE01814, userDetails.getLanguage());
        if (current.getEntity().getType().equals(AttributeType.Enumerated) && customAttributeDefinitionEntity.getDefElements() != null &&
                customAttributeDefinitionEntity.getDefElements().isEmpty())
            throw new LottabyteException(Message.LBE01816, userDetails.getLanguage());
        validateCustomAttributeDefinitionDefElements(customAttributeDefinitionEntity, userDetails);
        customAttributeDefinitionRepository.patchCustomAttributeDefinition(customAttributeDefinitionId, customAttributeDefinitionEntity, userDetails);
        if (current.getEntity().getType().equals(AttributeType.Enumerated) && customAttributeDefinitionEntity.getDefElements() != null) {
            for (CustomAttributeEnumValue ev : customAttributeDefinitionEntity.getDefElements()) {
                if (current.getEntity().getDefElements().stream().noneMatch(x -> x.getName().equals(ev.getName()))) {
                    customAttributeDefinitionRepository.createCustomAttributeDefElement(ev, customAttributeDefinitionId, userDetails);
                }
            }
            for (CustomAttributeEnumValue currentEv : current.getEntity().getDefElements()) {
                if (customAttributeDefinitionEntity.getDefElements().stream().noneMatch(x -> x.getName().equals(currentEv.getName()))) {
                    customAttributeDefinitionRepository.deleteCustomAttributeDefElementById(currentEv.getId(), userDetails);
                    customAttributeDefinitionRepository.deleteCustomAttributesByDefElementId(currentEv.getId(), customAttributeDefinitionId, userDetails);
                }
            }
        }
        return getCustomAttributeDefinitionById(customAttributeDefinitionId, userDetails);
    }

    private void validateCustomAttributeDefinitionDefElements(UpdatableCustomAttributeDefinitionEntity customAttributeDefinitionEntity, UserDetails userDetails) throws LottabyteException {
        Set<String> defElementNames = new HashSet<>();
        if (customAttributeDefinitionEntity.getType().equals(AttributeType.Enumerated) &&
                customAttributeDefinitionEntity.getDefElements() != null) {
            for (CustomAttributeEnumValue ev : customAttributeDefinitionEntity.getDefElements()) {
                if (ev.getName() == null || ev.getName().isEmpty())
                    throw new LottabyteException(Message.LBE01817, userDetails.getLanguage());
                if (!defElementNames.contains(ev.getName())) {
                    defElementNames.add(ev.getName());
                } else
                    throw new LottabyteException(Message.LBE01818, userDetails.getLanguage(), ev.getName());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ArchiveResponse deleteCustomAttributeDefinition(String customAttributeDefinitionId, UserDetails userDetails) throws LottabyteException {
        if(getCustomAttributeDefinitionById(customAttributeDefinitionId, userDetails).getId() == null){
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), customAttributeDefinitionId);
        }

        customAttributeDefinitionRepository.deleteCustomAttributeDefElementsByDefinitionId(customAttributeDefinitionId, userDetails);
        customAttributeDefinitionRepository.deleteCustomAttributesByDefinitionId(customAttributeDefinitionId, userDetails);
        customAttributeDefinitionRepository.deleteCustomAttributeDefinition(customAttributeDefinitionId, userDetails.getTenant());
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(customAttributeDefinitionId));
        return archiveResponse;
    }

    // Def Elements

    public CustomAttributeDefElement getCustomAttributeDefElementById(String customAttributeDefElementId, UserDetails userDetails) throws LottabyteException {
        CustomAttributeDefElement res = customAttributeDefinitionRepository.getCustomAttributeDefElementById(customAttributeDefElementId, userDetails);
        if (res == null)
            throw new LottabyteException(Message.LBE01807, userDetails.getLanguage(), customAttributeDefElementId);
        return res;
    }

    public PaginatedArtifactList<CustomAttributeDefElement> getAllCustomAttributeDefElementPaginated(Integer offset, Integer limit, String definitionId, UserDetails userDetails) {
        return customAttributeDefinitionRepository.getAllCustomAttributeDefElementPaginated(definitionId, offset, limit, userDetails);
    }

    public CustomAttributeDefElement createCustomAttributeDefElement(UpdatableCustomAttributeDefElementEntity newCustomAttributeDefElementEntity, UserDetails userDetails) throws LottabyteException {
        String id = null;
        if (newCustomAttributeDefElementEntity.getName() == null || newCustomAttributeDefElementEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01809, userDetails.getLanguage());
        if (newCustomAttributeDefElementEntity.getDefinitionId() == null)
            throw new LottabyteException(Message.LBE01801, userDetails.getLanguage());
        if (!customAttributeDefinitionRepository.existsById(newCustomAttributeDefElementEntity.getDefinitionId(), userDetails))
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), newCustomAttributeDefElementEntity.getDefinitionId());
        CustomAttributeDefinition current = getCustomAttributeDefinitionById(newCustomAttributeDefElementEntity.getDefinitionId(), userDetails);
        if (!current.getEntity().getType().equals(AttributeType.Enumerated))
            throw new LottabyteException(Message.LBE01819, userDetails.getLanguage());
        if (!current.getEntity().getDefElements().stream().anyMatch(x -> x.getName().equals(newCustomAttributeDefElementEntity.getName()))) {
            id = customAttributeDefinitionRepository.createCustomAttributeDefElement(newCustomAttributeDefElementEntity, userDetails);
        } else {
            CustomAttributeEnumValue ev =  current.getEntity().getDefElements().stream().filter(x -> x.getName().equals(newCustomAttributeDefElementEntity.getName()))
                    .findFirst().get();
            id = ev.getId();
        }
        return getCustomAttributeDefElementById(id, userDetails);
    }

    public CustomAttributeDefElement patchCustomAttributeDefElement(String customAttributeDefElementId, UpdatableCustomAttributeDefElementEntity customAttributeDefElementEntity, UserDetails userDetails) throws LottabyteException {
        if (customAttributeDefElementEntity.getName() != null && customAttributeDefElementEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01809, userDetails.getLanguage());
        if (!customAttributeDefinitionRepository.existsCustomAttributeDefElement(customAttributeDefElementId, userDetails))
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), customAttributeDefElementId);
        if (customAttributeDefElementEntity.getDefinitionId() != null)
            throw new LottabyteException(Message.LBE01820, userDetails.getLanguage());
        customAttributeDefinitionRepository.patchCustomAttributeDefElement(customAttributeDefElementId, customAttributeDefElementEntity, userDetails);
        return getCustomAttributeDefElementById(customAttributeDefElementId, userDetails);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ArchiveResponse deleteCustomAttributeDefElement(String customAttributeDefElementId, UserDetails userDetails) throws LottabyteException {
        if (!customAttributeDefinitionRepository.existsCustomAttributeDefElement(customAttributeDefElementId, userDetails))
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), customAttributeDefElementId);
        CustomAttributeDefElement current = customAttributeDefinitionRepository.getCustomAttributeDefElementById(customAttributeDefElementId, userDetails);
        customAttributeDefinitionRepository.deleteCustomAttributeDefElementById(customAttributeDefElementId, userDetails);
        customAttributeDefinitionRepository.deleteCustomAttributesByDefElementId(customAttributeDefElementId, current.getEntity().getDefinitionId(), userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(customAttributeDefElementId));
        return archiveResponse;
    }

    // Custom Attributes

    public List<CustomAttribute> getCustomAttributeByObjectId(String objectId, UserDetails userDetails) throws LottabyteException {
        List<CustomAttributeRecord> customAttributeRecordsList = customAttributeDefinitionRepository.getCustomAttributeRecordsByObjectId(objectId, userDetails.getTenant());
        Map<String, CustomAttribute> customAttributeMap = new HashMap<>();
        for(CustomAttributeRecord customAttributeRecord : customAttributeRecordsList){
            CustomAttributeDefinition customAttributeDefinition = getCustomAttributeDefinitionById(customAttributeRecord.getCustomAttributeDefinitionId(), userDetails);

            List<CustomAttributeValue<?>> customAttributeValues;
            if (!customAttributeMap.containsKey(customAttributeRecord.getCustomAttributeDefinitionId())) {
                CustomAttribute ca = new CustomAttribute();
                customAttributeMap.put(customAttributeRecord.getCustomAttributeDefinitionId(), ca);
                customAttributeValues = new ArrayList<>();
                ca.setValues(customAttributeValues);
                if(customAttributeDefinition != null) {
                    ca.setName(customAttributeDefinition.getEntity().getName());
                    ca.setCustomAttributeDefinitionId(customAttributeDefinition.getId());
                    ca.setObjectType(customAttributeDefinition.getEntity().getType());
                }
            } else {
                customAttributeValues = customAttributeMap.get(customAttributeRecord.getCustomAttributeDefinitionId()).getValues();
            }

            if (customAttributeRecord.getObjectType() != null && !customAttributeRecord.getObjectType().isEmpty()) {
                if (customAttributeDefinition.getEntity().getType().equals(AttributeType.String)) {
                    CustomAttributeValue<String> val = new CustomAttributeValue<>();
                    val.setCustomAttributeId(customAttributeRecord.getId());
                    val.setValue(customAttributeRecord.getTextValue());
                    customAttributeValues.add(val);
                } else if (customAttributeDefinition.getEntity().getType().equals(AttributeType.Numeric)) {
                    CustomAttributeValue<Double> val = new CustomAttributeValue<>();
                    val.setCustomAttributeId(customAttributeRecord.getId());
                    val.setValue(customAttributeRecord.getNumberValue());
                    customAttributeValues.add(val);
                } else if (customAttributeDefinition.getEntity().getType().equals(AttributeType.Date)) {
                    CustomAttributeValue<LocalDateTime> val = new CustomAttributeValue<>();
                    val.setCustomAttributeId(customAttributeRecord.getId());
                    val.setValue(customAttributeRecord.getDataValue());
                    customAttributeValues.add(val);
                } else if (customAttributeDefinition.getEntity().getType().equals(AttributeType.Enumerated)) {
                    CustomAttributeValue<CustomAttributeEnumValue> val = new CustomAttributeValue<>();
                    CustomAttributeEnumValue enumValue = new CustomAttributeEnumValue();
                    CustomAttributeDefElement customAttributeDefElement = getCustomAttributeDefElementById(customAttributeRecord.getDefElementId(), userDetails);
                    enumValue.setId(customAttributeDefElement.getId());
                    enumValue.setName(customAttributeDefElement.getEntity().getName());
                    enumValue.setDescription(customAttributeDefElement.getEntity().getDescription());
                    val.setCustomAttributeId(customAttributeRecord.getId());
                    val.setValue(enumValue);
                    customAttributeValues.add(val);
                }
            }
        }
        return customAttributeMap.values().stream().sorted(Comparator.comparing(CustomAttribute::getName)).collect(Collectors.toList());
    }

    public void validateCustomAttribute(CustomAttribute ca, Set<String> caDefIds, UserDetails userDetails) throws LottabyteException {

        /// !!!!!!!!!!!
        /// TODO: Multivalue Validation
        /// !!!!!!!!!!!!1
        if (ca.getCustomAttributeDefinitionId() == null || ca.getCustomAttributeDefinitionId().isEmpty())
            throw new LottabyteException(Message.LBE01801, userDetails.getLanguage());
        CustomAttributeDefinition caDef = customAttributeDefinitionRepository
                .getById(ca.getCustomAttributeDefinitionId(), userDetails);
        if (caDef == null)
            throw new LottabyteException(Message.LBE01802, userDetails.getLanguage(), ca.getCustomAttributeDefinitionId());
        if (!caDefIds.contains(caDef.getId())) {
            caDefIds.add(caDef.getId());
        } else {
            throw new LottabyteException(Message.LBE01806, userDetails.getLanguage(), ca.getCustomAttributeDefinitionId());
        }
        /*if (ca.getValues() == null || ca.getValues().isEmpty())
            throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.format(Message.LBE01803.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), ca.getCustomAttributeDefinitionId()));
        */
        for (CustomAttributeValue caVal : ca.getValues()) {
            if (caDef.getEntity().getType().equals(AttributeType.Enumerated)) {
                if (!(caVal.getValue() instanceof LinkedHashMap) ||
                        ((LinkedHashMap) caVal.getValue()).getOrDefault("id", null) == null)
                    throw new LottabyteException(Message.LBE01804, userDetails.getLanguage(), ca.getCustomAttributeDefinitionId());
                String enumValId = ((LinkedHashMap) caVal.getValue()).get("id").toString();
                if (enumValId == null || enumValId.isEmpty())
                    throw new LottabyteException(Message.LBE01804, userDetails.getLanguage(), ca.getCustomAttributeDefinitionId());
                if (!customAttributeDefinitionRepository.existsCustomAttributeDefElement(caDef.getId(), enumValId, userDetails))
                    throw new LottabyteException(Message.LBE01805, userDetails.getLanguage(), enumValId,
                            ca.getCustomAttributeDefinitionId());
            }
        }
    }

    public void createCustomAttributes(List<CustomAttribute> caList, String objectId, String objectType, UserDetails userDetails,
                                       Boolean doValidation) throws LottabyteException {
        if (caList != null && !caList.isEmpty()) {
            Set<String> caDefIds = new HashSet<>();
            for (CustomAttribute ca : caList) {
                if (doValidation)
                    validateCustomAttribute(ca, caDefIds, userDetails);
                CustomAttributeDefinition caDef = customAttributeDefinitionRepository
                        .getById(ca.getCustomAttributeDefinitionId(), userDetails);
                customAttributeDefinitionRepository.createCustomAttribute(ca, caDef, objectId, objectType, userDetails);
            }
        }
    }

    public void patchCustomAttributes(List<CustomAttribute> caList, String objectId, String objectType, UserDetails userDetails,
                                      Boolean doValidation) throws LottabyteException {
        List<CustomAttribute> current = getCustomAttributeByObjectId(objectId, userDetails);
        if (caList != null) {
            if (caList.isEmpty()) {
                // Remove all custom attributes
                customAttributeDefinitionRepository.deleteAllCustomAttributesByArtifactId(objectId, userDetails);
            } else {
                Set<String> caDefIds = new HashSet<>();
                for (CustomAttribute ca : caList) {
                    if (doValidation)
                        validateCustomAttribute(ca, caDefIds, userDetails);
                    CustomAttributeDefinition caDef = customAttributeDefinitionRepository
                            .getById(ca.getCustomAttributeDefinitionId(), userDetails);
                    if (current.stream().anyMatch(x -> x.getCustomAttributeDefinitionId().equals(caDef.getId()))) {
                        // Modify custom attribute
                        customAttributeDefinitionRepository.patchCustomAttribute(ca, caDef, current, objectId, objectType, userDetails);
                    } else {
                        // Create new custom attribute
                        customAttributeDefinitionRepository.createCustomAttribute(ca, caDef, objectId, objectType, userDetails);
                    }
                }
                for (CustomAttribute caCurr : current) {
                    // Check current custom attributes, remove items not found in patch
                    if (!caList.stream().anyMatch(x -> x.getCustomAttributeDefinitionId().equals(caCurr.getCustomAttributeDefinitionId())))
                        customAttributeDefinitionRepository.deleteCustomAttributesByArtifactIdAndDefinitionId(
                                objectId, caCurr.getCustomAttributeDefinitionId(), userDetails);
                }
            }
        }
    }

    public void deleteAllCustomAttributesByArtifactId(String artifactId, UserDetails userDetails) {
        customAttributeDefinitionRepository.deleteAllCustomAttributesByArtifactId(artifactId, userDetails);
    }

    public void copyCustomAttributes(String sourceArtifactId,
                                     String targetArtifactId, ArtifactType targetArtifactType,
                                     UserDetails userDetails) throws LottabyteException {
        customAttributeDefinitionRepository.copyCustomAttributes(sourceArtifactId, targetArtifactId, targetArtifactType, userDetails);
    }
    
}
