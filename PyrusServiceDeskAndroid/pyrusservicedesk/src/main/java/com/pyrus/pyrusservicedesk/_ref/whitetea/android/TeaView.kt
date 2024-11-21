package com.pyrus.pyrusservicedesk._ref.whitetea.android

import com.pyrus.pyrusservicedesk._ref.whitetea.core.EffectHandler
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewMessages
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer

internal interface TeaView<Model : Any, Message : Any, Effect : Any> :
    ViewRenderer<Model>,
    EffectHandler<Effect>,
    ViewMessages<Message>