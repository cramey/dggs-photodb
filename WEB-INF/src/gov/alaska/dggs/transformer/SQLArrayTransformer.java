package gov.alaska.dggs.transformer;

import flexjson.BasicType;
import flexjson.TypeContext;
import flexjson.transformer.AbstractTransformer;
import java.lang.reflect.Array;


// Transforms SQL Arrays into proper arrays
public class SQLArrayTransformer extends AbstractTransformer
{
	@Override
	public void transform(Object object)
	{
		try {
			java.sql.Array sql_arr = (java.sql.Array)object;
			Object arr = sql_arr.getArray();

			TypeContext typeContext = getContext().writeOpenArray();
			int length = Array.getLength(arr);
			for(int i = 0; i < length; ++i){
				if(!typeContext.isFirst()) getContext().writeComma();
				typeContext.setFirst(false);
				getContext().transform(Array.get(arr, i));
			}
			getContext().writeCloseArray();
		} catch(Exception ex){ }
	}
}
