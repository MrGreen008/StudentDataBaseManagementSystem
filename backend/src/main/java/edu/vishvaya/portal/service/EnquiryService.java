package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.EnquiryReceipt;
import edu.vishvaya.portal.api.EnquiryRequest;

public interface EnquiryService {

    EnquiryReceipt submit(EnquiryRequest request);
}
