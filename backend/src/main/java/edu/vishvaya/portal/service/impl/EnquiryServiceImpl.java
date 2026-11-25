package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.EnquiryReceipt;
import edu.vishvaya.portal.api.EnquiryRequest;
import edu.vishvaya.portal.persistence.EnquiryRepository;
import edu.vishvaya.portal.service.EnquiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnquiryServiceImpl implements EnquiryService {

    private static final Logger log = LoggerFactory.getLogger(EnquiryServiceImpl.class);

    private final EnquiryRepository repository;

    public EnquiryServiceImpl(EnquiryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EnquiryReceipt submit(EnquiryRequest request) {
        EnquiryReceipt receipt = repository.save(request);
        log.info("Admission enquiry saved with reference {}", receipt.reference());
        return receipt;
    }
}
