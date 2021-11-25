package life.genny.bridge.model;


public class AttributeCodeValueString {
	public String attributeCode;
	public String value;
	
	public AttributeCodeValueString(final String attributeCode, final String value)
	{
		this.attributeCode = attributeCode;
		this.value = value;
	}

	/**
	 * @return the attributeCode
	 */
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	
	
}