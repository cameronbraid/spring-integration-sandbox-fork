package org.springframework.integration.smpp;

/**
 * stores SMPP Spring Integration message header constants
 *
 * @author Josh Long
 */
public abstract class SmppConstants {

	static public final String SRC_ADDR = "src-address",
			DST_ADDR = "dst-address",
			SMS_MSG = "sms-short-message";

	static public final String
			REGISTERED_DELIVERY_MODE = "registered-delivery-mode",
			REPLACE_IF_PRESENT_FLAG = "replace-if-present-flag",
			DATA_CODING = "data-coding",
			SM_DEFAULT_MSG_ID = "sm-default-msg-id";

	static public final String SERVICE_TYPE = "service-type",
			SRC_TON = "src-addr-ton",
			DST_TON = "dst-addr-ton",
			DST_NPI = "dst-addr-npi",
			SRC_NPI = "src-addr-npi";

	static public final String ESM_CLASS = "esm-class",
			PROTOCOL_ID = "protocol-id", PRIORITY_FLAG = "priority-flag",
			SCHEDULED_DELIVERY_TIME = "scheduled-delivery-time",
			VALIDITY_PERIOD = "validity-period";
}
