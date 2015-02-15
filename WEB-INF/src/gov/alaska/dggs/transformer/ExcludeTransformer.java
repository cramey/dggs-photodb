package gov.alaska.dggs.transformer;

import flexjson.transformer.AbstractTransformer;

// Ignores transformations entirely
public class ExcludeTransformer extends AbstractTransformer
{
	@Override
	public Boolean isInline(){ return true; }

	@Override
	public void transform(Object object){ return; }
}
