package ca.utoronto.utm.mcs;

import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PostModule.class)
public interface ServletComponent {

	public ServletHandler buildServletHandler();
}

