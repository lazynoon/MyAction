package net_io.mixed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {
	String name() default "";
	boolean required() default false;
	JsonField.Access access() default JsonField.Access.AUTO;

	public static enum Access {
		AUTO,
		READ_ONLY,
		WRITE_ONLY,
		READ_WRITE;

		private Access() {
		}
	}

}
