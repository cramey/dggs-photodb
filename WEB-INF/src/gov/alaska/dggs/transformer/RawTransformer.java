package gov.alaska.dggs.transformer;

import flexjson.transformer.AbstractTransformer;
import flexjson.TypeContext;
import flexjson.JSONContext;

// Takes in a string and places it, unchanged, into the json.
// This is only appropriate for string that already contain proper
// json
public class RawTransformer extends AbstractTransformer
{
	@Override
	public Boolean isInline(){ return true; }

	@Override
	public void transform(Object o){
		// Do nothing to null values
		if(o == null) return;
		
		JSONContext ctx = getContext();
		TypeContext tc = ctx.peekTypeContext();

		if(!tc.isFirst()) ctx.writeComma();
		ctx.writeName(tc.getPropertyName());
		ctx.write((String)o);
	}
}
