package life.genny;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

import life.genny.bridge.model.AttributeCodeValueString;
import life.genny.bridge.model.GennyItem;
import life.genny.bridge.model.QDataB2BMessage;

public class b2btest {

	@Test
	public void b2bTest()
	{
		List<GennyItem> gennyItems = new ArrayList<GennyItem>();

		// go through query parameters and add them to a GennyItem
		GennyItem gennyItem = new GennyItem();
			AttributeCodeValueString attCodevs = new AttributeCodeValueString("PRI_FIRSTNAME", "James");
			gennyItem.addB2B(attCodevs);

			attCodevs = new AttributeCodeValueString("PRI_LASTNAME", "Bond");
			gennyItem.addB2B(attCodevs);


		attCodevs = new AttributeCodeValueString("PRI_USERNAME", "username");
		gennyItem.addB2B(attCodevs);
		attCodevs = new AttributeCodeValueString("PRI_USERCODE", "usercode");
		gennyItem.addB2B(attCodevs);

		gennyItems.add(gennyItem);

		QDataB2BMessage dataMsg = new QDataB2BMessage(gennyItems.toArray(new GennyItem[0]));
		dataMsg.setAliasCode("STATELESS");

		Jsonb jsonb = JsonbBuilder.create();
		System.out.println(jsonb.toJson(dataMsg));
	}
}
