(() => {
    'use strict';

    const deliveryDate = document.querySelector('#delivery-date');
    const orderForm = document.querySelector('#order-form');
    const formStatus = document.querySelector('#form-status');
    const orderResult = document.querySelector('#order-result');
    const orderSummary = document.querySelector('#order-summary');
    const whatsappLink = document.querySelector('#whatsapp-link');

    if (!deliveryDate || !orderForm || !formStatus || !orderResult || !orderSummary || !whatsappLink) {
        return;
    }

    const whatsappNumber = '573183074075';

    const getNextSunday = () => {
        const sunday = new Date();
        const daysUntilSunday = (7 - sunday.getDay()) % 7 || 7;

        sunday.setDate(sunday.getDate() + daysUntilSunday);
        return sunday;
    };

    const capitalize = (value) => value.charAt(0).toUpperCase() + value.slice(1);

    const nextSunday = getNextSunday();
    const formattedDate = new Intl.DateTimeFormat('es-CO', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    }).format(nextSunday);

    deliveryDate.textContent = formattedDate;

    orderForm.addEventListener('submit', (event) => {
        event.preventDefault();
        formStatus.textContent = '';
        orderResult.hidden = true;

        if (!orderForm.reportValidity()) {
            return;
        }

        const formData = new FormData(orderForm);
        const tigrilloQuantity = Number(formData.get('tigrilloQuantity')) || 0;
        const bolonQuantity = Number(formData.get('bolonQuantity')) || 0;

        if (tigrilloQuantity === 0 && bolonQuantity === 0) {
            formStatus.textContent = 'Selecciona al menos un tigrillo o un bolón para continuar.';
            return;
        }

        const customerName = String(formData.get('customerName')).trim();
        const customerPhone = String(formData.get('customerPhone')).trim();
        const deliveryAddress = String(formData.get('deliveryAddress')).trim();
        const deliveryReference = String(formData.get('deliveryReference')).trim();
        const selectedItems = [];

        if (tigrilloQuantity > 0) {
            selectedItems.push(`${tigrilloQuantity} tigrillo${tigrilloQuantity === 1 ? '' : 's'}`);
        }

        if (bolonQuantity > 0) {
            selectedItems.push(bolonQuantity === 1 ? '1 bolón' : `${bolonQuantity} bolones`);
        }

        const referenceLine = deliveryReference
            ? `Referencia: ${deliveryReference}`
            : 'Referencia: No especificada';

        const message = [
            'Hola, quiero confirmar un pedido solidario de ECUADOR-IN.',
            `Fecha de entrega: ${formattedDate}.`,
            `Nombre: ${customerName}.`,
            `Teléfono: ${customerPhone}.`,
            `Dirección: ${deliveryAddress}.`,
            referenceLine + '.',
            `Pedido: ${selectedItems.join(' y ')}.`
        ].join('\n');

        whatsappLink.href = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`;
        orderSummary.textContent = `${capitalize(customerName)}, recibirás ${selectedItems.join(' y ')} el ${formattedDate} en ${deliveryAddress}.`;
        orderResult.hidden = false;
        orderResult.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
})();
